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
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.core.dom.IAnnotationBinding;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.internal.compiler.lookup.LocalTypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.MethodBinding;
import org.eclipse.jdt.internal.compiler.lookup.SyntheticMethodBinding;
import org.eclipse.jdt.internal.compiler.lookup.TypeBinding;
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
	private final TypeBinding internalBinding;
	private final Boolean defaultNonNullState;

	NullnessAssertionInserter(ClassVisitor classVisitor, ITypeBinding typeBinding) {
		super(classVisitor);
		this.typeBinding = typeBinding;
		this.internalBinding = getInternalBinding(typeBinding);
		this.defaultNonNullState = getDefaultNonNullState();
	}

	boolean isCheckInserted() {
		return checkInserted;
	}

	private Boolean getDefaultNonNullState() {
		Boolean result = recursiveGetDefaultNonNullState(typeBinding);
		return result;
	}

	private Boolean getDefaultNonNullState(IBinding binding) {
		IAnnotationBinding[] annotations = binding.getAnnotations();
		for (IAnnotationBinding annotation : annotations) {
			if ("org.eclipse.jdt.annotation.NonNullByDefault".equals(getAnnotationTypeName(annotation))) {
				// TODO check the value of 'value'
				return Boolean.TRUE;
			}
		}
		return null;
	}

	private Boolean getEffectiveNonNullState(IMethodBinding binding) {
		IAnnotationBinding[] annotations = binding.getAnnotations();
		for (IAnnotationBinding annotation : annotations) {
			if ("org.eclipse.jdt.annotation.NonNull".equals(getAnnotationTypeName(annotation))) {
				// TODO check the value of 'value'
				return Boolean.TRUE;
			}
			if ("org.eclipse.jdt.annotation.NonNullByDefault".equals(getAnnotationTypeName(annotation))) {
				return Boolean.TRUE;
			}
			if ("org.eclipse.jdt.annotation.Nullable".equals(getAnnotationTypeName(annotation))) {
				return Boolean.FALSE;
			}
		}
		return defaultNonNullState;
	}

	private Boolean getEffectiveNonNullState(IMethodBinding binding, IAnnotationBinding[] parameterAnnotations) {
		for (IAnnotationBinding annotation : parameterAnnotations) {
			if ("org.eclipse.jdt.annotation.NonNull".equals(getAnnotationTypeName(annotation))) {
				return Boolean.TRUE;
			}
			if ("org.eclipse.jdt.annotation.Nullable".equals(getAnnotationTypeName(annotation))) {
				return Boolean.FALSE;
			}
		}
		Boolean methodDefault = getDefaultNonNullState(binding);
		if (methodDefault != null)
			return methodDefault;
		return defaultNonNullState;
	}

	private String getAnnotationTypeName(IAnnotationBinding annotation) {
		return annotation.getAnnotationType().getQualifiedName();
	}

	private Boolean recursiveGetDefaultNonNullState(IBinding binding) {
		Boolean result = getDefaultNonNullState(binding);
		if (result == null) {
			if (binding instanceof IMethodBinding) {
				result = recursiveGetDefaultNonNullState(((IMethodBinding) binding).getDeclaringClass());
			} else if (binding instanceof IVariableBinding) {
				result = recursiveGetDefaultNonNullState(((IVariableBinding) binding).getDeclaringClass());
			} else if (binding instanceof ITypeBinding) {
				ITypeBinding castedBinding = (ITypeBinding) binding;
				if (castedBinding.isMember()) {
					result = recursiveGetDefaultNonNullState(castedBinding.getDeclaringClass());
				} else if (castedBinding.isLocal()) {
					result = recursiveGetDefaultNonNullState(castedBinding.getDeclaringMethod());
				} else {
					result = recursiveGetDefaultNonNullState(castedBinding.getPackage());
				}
			}
		}
		return result;
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

	private IMethodBinding findMethod(String name, String desc) {
		char[] descAsArray = desc != null ? desc.toCharArray() : null;
		IMethodBinding[] methods = typeBinding.getDeclaredMethods();
		try {
			for (IMethodBinding method : methods) {
				if (name.equals(method.getName()) || CONSTRUCTOR.equals(name) && method.isConstructor()) {
					MethodBinding candidate = getInternalBinding(method);
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
		return methodBinding.isSynthetic() || getInternalBinding(methodBinding) instanceof SyntheticMethodBinding;
	}

	@Override
	public MethodVisitor visitMethod(final int access, final String name, String desc, String signature, String[] exceptions) {
		MethodVisitor methodVisitor = cv.visitMethod(access, name, desc, signature, exceptions);
		if ((access & ACC_SYNTHETIC) != 0) {
			return methodVisitor;
		}
		IMethodBinding methodBinding = findMethod(name, desc);
		if (methodBinding == null || isEffectivelySynthetic(methodBinding)) {
			return methodVisitor;
		}
		final Type[] args = Type.getArgumentTypes(desc);
		Type returnType = Type.getReturnType(desc);

		// null == undefined, false == nullable, true == non-null
		final Boolean[] nonNullParameters = new Boolean[methodBinding.getParameterTypes().length];
		final Boolean[] returnTypeNonNull = new Boolean[1];

		for (int i = 0; i < nonNullParameters.length; i++) {
			nonNullParameters[i] = getEffectiveNonNullState(methodBinding, methodBinding.getParameterAnnotations(i));
		}
		returnTypeNonNull[0] = getEffectiveNonNullState(methodBinding);
		final boolean hasNonNullParameter = hasNonNullParameter(nonNullParameters);
		final String[] parameterNames = hasNonNullParameter ? getParameterNames(methodBinding) : null;
		return new MethodAdapter(methodVisitor) {
			private Label throwLabel;
			private Label startGeneratedCodeLabel;

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
					if (Boolean.TRUE.equals(returnTypeNonNull[0])) {
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
