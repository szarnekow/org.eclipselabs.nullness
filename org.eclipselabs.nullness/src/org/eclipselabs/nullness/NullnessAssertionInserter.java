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

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.LocalTypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.MethodBinding;
import org.eclipse.jdt.internal.compiler.lookup.SyntheticMethodBinding;
import org.eclipse.jdt.internal.compiler.lookup.TypeConstants;
import org.eclipselabs.nullness.jdt.Bindings;
import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodAdapter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

class NullnessAssertionInserter extends ClassAdapter {
	private static final String ILLEGAL_ARGUMENT_EXCEPTION = Type.getInternalName(IllegalArgumentException.class);
	private static final String ILLEGAL_STATE_EXCEPTION = Type.getInternalName(IllegalStateException.class);
	private static final String CONSTRUCTOR = String.valueOf(TypeConstants.INIT);
	private static final String STATIC_INITIALIZER = String.valueOf(TypeConstants.CLINIT);

	private boolean checkInserted = false;

	private final ITypeBinding typeBinding;
	private final Boolean defaultNonNullState;
	private final NullAnnotationFinder annotationFinder;
	private final FieldCache fieldCache;

	NullnessAssertionInserter(ClassVisitor classVisitor, ITypeBinding typeBinding, NullAnnotationFinder annotationFinder,
			FieldCache fieldCache) {
		super(classVisitor);
		this.typeBinding = typeBinding;
		this.annotationFinder = annotationFinder;
		this.fieldCache = fieldCache;
		this.defaultNonNullState = annotationFinder.getDefaultNonNullState(typeBinding);
	}

	boolean isCheckInserted() {
		return checkInserted;
	}

	private IMethodBinding findMethod(String name, String desc) {
		if (STATIC_INITIALIZER.equals(name)) {
			return null;
		}
		char[] descAsArray = desc != null ? desc.toCharArray() : null;
		IMethodBinding[] methods = typeBinding.getDeclaredMethods();
		try {
			for (IMethodBinding method : methods) {
				if (name.equals(method.getName()) || CONSTRUCTOR.equals(name) && method.isConstructor()) {
					MethodBinding candidate = Bindings.getInternalBinding(method);
					if (CharOperation.equals(descAsArray, candidate.signature())) {
						return method;
					}
				}
			}
			return null;
		} catch (Exception e) {
			return null;
		}
	}

	private String[] getParameterNames(IMethodBinding methodBinding) {
		if (methodBinding == null)
			return new String[0];
		try {
			IMethod javaMethod = (IMethod) methodBinding.getJavaElement();
			if (javaMethod == null) {
				return new String[0];
			}
			return javaMethod.getParameterNames();
		} catch (JavaModelException e) {
			return new String[methodBinding.getParameterTypes().length];
		}
	}

	private boolean hasNonNullParameter(Boolean[] nonNullState) {
		for (int i = 0; i < nonNullState.length; i++) {
			if (Boolean.TRUE.equals(nonNullState[i]))
				return true;
		}
		return false;
	}

	private boolean isEffectivelySynthetic(IMethodBinding methodBinding) {
		return methodBinding.isSynthetic() || (Bindings.getInternalBinding(methodBinding) instanceof SyntheticMethodBinding);
	}

	@Override
	public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
		FieldVisitor fieldVisitor = cv.visitField(access, name, desc, signature, value);

		return fieldVisitor;
	}

	@Override
	public MethodVisitor visitMethod(final int access, final String name, String desc, String signature, String[] exceptions) {
		MethodVisitor methodVisitor = cv.visitMethod(access, name, desc, signature, exceptions);
		if ((access & ACC_SYNTHETIC) != 0) {
			return methodVisitor;
		}
		IMethodBinding methodBinding = findMethod(name, desc);
		if (methodBinding != null && isEffectivelySynthetic(methodBinding)) {
			return methodVisitor;
		}
		final Type[] args = Type.getArgumentTypes(desc);

		// null == undefined, false == nullable, true == non-null
		final Boolean[] nonNullParameters = methodBinding != null ? new Boolean[methodBinding.getParameterTypes().length] : new Boolean[0];
		final Boolean[] returnTypeNonNull = new Boolean[1];

		if (methodBinding != null) {
			for (int i = 0; i < nonNullParameters.length; i++) {
				nonNullParameters[i] = annotationFinder.getEffectiveNonNullState(methodBinding, methodBinding.getParameterAnnotations(i),
						defaultNonNullState);
			}
			returnTypeNonNull[0] = annotationFinder.getEffectiveNonNullState(methodBinding, defaultNonNullState);
		}
		final boolean hasNonNullParameter = hasNonNullParameter(nonNullParameters);
		final String[] parameterNames = hasNonNullParameter ? getParameterNames(methodBinding) : null;
		return new MethodAdapter(methodVisitor) {
			private Label throwReturnLabel;
			private Label startGeneratedCodeLabel;
			private Map<String, Label> fieldAccessLabels;

			@Override
			public void visitCode() {
				if (hasNonNullParameter) {
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
									"Argument for non-null parameter %s at index %d of %s#%s must not be null", parameterNames[param],
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
					LocalTypeBinding localType = Bindings.getInternalBinding(typeBinding);
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
					if (Boolean.TRUE.equals(returnTypeNonNull[0])) {
						mv.visitInsn(DUP);
						if (throwReturnLabel == null) {
							Label skipLabel = new Label();
							mv.visitJumpInsn(IFNONNULL, skipLabel);
							throwReturnLabel = new Label();
							mv.visitLabel(throwReturnLabel);
							throwStatement(ILLEGAL_STATE_EXCEPTION,
									String.format("Non-null method %s#%s must not return null", getTypeName(), name), skipLabel);
						} else {
							mv.visitJumpInsn(IFNULL, throwReturnLabel);
						}
					}
				}

				mv.visitInsn(opcode);
			}

			@Override
			public void visitFieldInsn(int opcode, String owner, String name, String desc) {
				if (opcode == PUTFIELD || opcode == PUTSTATIC) {
					Boolean nullState = fieldCache.getEffectiveNonNullState(owner, name);
					if (Boolean.TRUE.equals(nullState)) {
						mv.visitInsn(DUP);
						if (fieldAccessLabels == null) {
							fieldAccessLabels = new HashMap<String, Label>(3);
						}
						String selector = name + "/" + owner;
						Label label = fieldAccessLabels.get(selector);
						if (label == null) {
							Label skipLabel = new Label();
							mv.visitJumpInsn(IFNONNULL, skipLabel);
							label = new Label();
							mv.visitLabel(label);
							throwStatement(ILLEGAL_STATE_EXCEPTION,
									String.format("Non-null field %s#%s must not be set to null", owner, name), skipLabel);
							fieldAccessLabels.put(selector, label);
						} else {
							mv.visitJumpInsn(IFNULL, label);
						}
					}
				}
				mv.visitFieldInsn(opcode, owner, name, desc);
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
