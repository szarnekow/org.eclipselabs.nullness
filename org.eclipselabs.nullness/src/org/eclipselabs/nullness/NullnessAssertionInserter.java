/*******************************************************************************
 * Copyright (c) 2012 Sebastian Zarnekow (http://zarnekow.blogspot.de) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Authors:
 *   Sebastian Zarnekow - Initial implementation
 *******************************************************************************/
package org.eclipselabs.nullness;

import static org.objectweb.asm.Opcodes.*;

import java.lang.reflect.Field;

import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.LocalTypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.TagBits;
import org.eclipse.jdt.internal.compiler.lookup.TypeConstants;
import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodAdapter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

class NullnessAssertionInserter extends ClassAdapter {
	private static final String ILLEGAL_ARGUMENT_EXCEPTION = Type.getInternalName(IllegalArgumentException.class);
	private static final String ILLEGAL_STATE_EXCEPTION = Type.getInternalName(IllegalStateException.class);
	private static final String CONSTRUCTOR = String.valueOf(TypeConstants.INIT);

	private boolean checkInserted = false;

	private final ITypeBinding typeBinding;

	NullnessAssertionInserter(ClassVisitor classVisitor, ITypeBinding typeBinding) {
		super(classVisitor);
		this.typeBinding = typeBinding;
	}

	boolean isCheckInserted() {
		return checkInserted;
	}

	private <T> T getInternalBinding(IBinding binding) {
		try {
			Field field = binding.getClass().getDeclaredField("binding"); //$NON-NLS-1$
			field.setAccessible(true);
			@SuppressWarnings("unchecked")
			T result = (T) field.get(binding);
			return result;
		} catch (NoSuchFieldException e) {
			return null;
		} catch (IllegalAccessException e) {
			return null;
		}
	}

	@Override
	public MethodVisitor visitMethod(final int access, final String name, String desc, String signature, String[] exceptions) {
		final Type[] args = Type.getArgumentTypes(desc);

		MethodVisitor v = cv.visitMethod(access, name, desc, signature, exceptions);
		IMethodBinding[] methods = typeBinding.getDeclaredMethods();
		String[] parameterNames = null;
		org.eclipse.jdt.internal.compiler.lookup.MethodBinding actualMethod = null;
		try {
			for (IMethodBinding method : methods) {
				if (name.equals(method.getName()) || CONSTRUCTOR.equals(name) && method.isConstructor()) {
					org.eclipse.jdt.internal.compiler.lookup.MethodBinding candidate = getInternalBinding(method);
					char[] candidateSignature = candidate.signature();
					boolean equal = desc == null && candidateSignature == null;
					if (!equal && desc != null && candidateSignature != null) {
						equal = desc.equals(String.valueOf(candidateSignature));
					}
					if (equal) {
						IMethod javaMethod = (IMethod) method.getJavaElement();
						if (javaMethod != null) {
							parameterNames = javaMethod.getParameterNames();
						}
						actualMethod = candidate;
						break;
					}
				}
			}
		} catch (Exception e) {
			// TODO proper exception handling
			e.printStackTrace();
			return v;
		}
		if (actualMethod == null) {
			return v;
		}
		long nonNullBits = TagBits.AnnotationNonNull | TagBits.AnnotationNonNullByDefault;
		final boolean nonNull = (actualMethod.getAnnotationTagBits() & nonNullBits) != 0;
		final Boolean[] nonNullParameters = actualMethod.parameterNonNullness;
		final boolean[] hasNonNullParameter = new boolean[] { false };
		if (nonNullParameters != null) {
			for (Boolean nonNullParameter : nonNullParameters) {
				if (Boolean.TRUE.equals(nonNullParameter)) {
					hasNonNullParameter[0] = true;
					break;
				}
			}
		}
		if (!hasNonNullParameter[0] && !nonNull) {
			return v;
		}
		final String[] finalParameterNames = parameterNames;
		return new MethodAdapter(v) {
			private Label throwLabel;
			private Label startGeneratedCodeLabel;

			@Override
			public void visitCode() {
				if (hasNonNullParameter[0]) {
					startGeneratedCodeLabel = new Label();
					mv.visitLabel(startGeneratedCodeLabel);
					for (int param = 0; param < nonNullParameters.length; param++) {
						if (Boolean.TRUE.equals(nonNullParameters[param])) {
							int var = ((access & ACC_STATIC) == 0) ? 1 : 0;
							int offset = args.length - nonNullParameters.length;
							for (int i = 0; i < param + offset; ++i) {
								var += args[i].getSize();
							}
							mv.visitVarInsn(ALOAD, var);

							Label end = new Label();
							mv.visitJumpInsn(IFNONNULL, end);

							throwStatement(ILLEGAL_ARGUMENT_EXCEPTION, String.format(
									"Argument for non-null parameter %s at index %d of %s#%s must not be null", finalParameterNames[param],
									param, getTypeName(), name), end);
						}
					}
				}
			}

			protected String getTypeName() {
				String result = typeBinding.getName();
				if (result != null && result.length() > 0) {
					return result;
				}
				if (typeBinding.isLocal()) {
					LocalTypeBinding localType = getInternalBinding(typeBinding);
					return "anonymous " + String.valueOf(localType.anonymousOriginalSuperType().shortReadableName());
				}
				return result;
			}

			@Override
			public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index) {
				boolean isStatic = (access & ACC_STATIC) != 0;
				boolean isParameter = isStatic ? index < args.length : index <= args.length;
				mv.visitLocalVariable(name, desc, signature, (isParameter && startGeneratedCodeLabel != null) ? startGeneratedCodeLabel
						: start, end, index);
			}

			@Override
			public void visitInsn(int opcode) {
				if (opcode == ARETURN) {
					if (nonNull) {
						mv.visitInsn(DUP);
						if (throwLabel == null) {
							Label skipLabel = new Label();
							mv.visitJumpInsn(IFNONNULL, skipLabel);
							throwLabel = new Label();
							mv.visitLabel(throwLabel);
							throwStatement(ILLEGAL_STATE_EXCEPTION,
									String.format("Non-null method %s#%s must not return null", getTypeName(), name), skipLabel);
						} else {
							mv.visitJumpInsn(IFNULL, throwLabel);
						}
					}
				}

				mv.visitInsn(opcode);
			}

			private void throwStatement(String exceptionType, String message, Label label) {
				Type stringType = Type.getType(String.class);
				String descriptor = Type.getMethodDescriptor(Type.VOID_TYPE, new Type[] { stringType });
				mv.visitTypeInsn(NEW, exceptionType);
				mv.visitInsn(DUP);
				mv.visitLdcInsn(message);
				mv.visitMethodInsn(INVOKESPECIAL, exceptionType, CONSTRUCTOR, descriptor);
				mv.visitInsn(ATHROW);
				mv.visitLabel(label);
				checkInserted = true;
			}
		};
	}

}
