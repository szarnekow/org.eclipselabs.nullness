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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IRegion;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.compiler.BuildContext;
import org.eclipse.jdt.core.compiler.CompilationParticipant;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipselabs.nullness.equinox.JavaProjectAnnotationSet;
import org.eclipselabs.nullness.equinox.MergedAnnotationSet;
import org.eclipselabs.nullness.equinox.NullnessAnnotationSet;
import org.eclipselabs.nullness.equinox.RegistryAnnotationSet;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.EmptyVisitor;

public class NullnessCompiler extends CompilationParticipant {

	private static final Logger log = Logger.getLogger(NullnessCompiler.class);

	private List<BuildContext> files;

	private final NullnessAnnotationSet[] defaultAnnotationSets;

	private NullnessAnnotationSet projectAnnotationSet;

	public NullnessCompiler() {
		this.defaultAnnotationSets = RegistryAnnotationSet.getAnnotationSets();
	}

	@Override
	public int aboutToBuild(IJavaProject project) {
		projectAnnotationSet = new JavaProjectAnnotationSet(project);
		return super.aboutToBuild(project);
	}

	@Override
	public void buildStarting(BuildContext[] files, boolean isBatch) {
		if (this.files != null) {
			this.files.addAll(Arrays.asList(files));
		} else {
			this.files = new ArrayList<BuildContext>(Arrays.asList(files));
		}
	}

	@Override
	public boolean isActive(IJavaProject project) {
		Boolean result = new PreferencesAccessor().getBoolean(PreferencesAccessor.ACTIVE_BOOLEAN, project.getProject());
		if (result != null) {
			return result.booleanValue();
		}
		return false;
	}

	@Override
	public void buildFinished(IJavaProject project) {
		try {
			super.buildFinished(project);
			if (files != null) {
				NullAnnotationFinder finder = new NullAnnotationFinder(new MergedAnnotationSet(projectAnnotationSet, defaultAnnotationSets));
				FieldCache fieldCache = new FieldCache(project, finder);
				for (BuildContext file : files) {
					try {
						addRuntimeChecks(file.getFile(), finder, fieldCache);
					} catch (JavaModelException e) {
						NullnessCompiler.log.error(e.getMessage(), e);
					}
				}
			}
		} finally {
			files = null;
			projectAnnotationSet = null;
		}
	}

	private void addRuntimeChecks(IFile javaFile, NullAnnotationFinder finder, FieldCache fieldCache) throws JavaModelException {
		IRegion region = JavaCore.newRegion();
		ICompilationUnit cu = (ICompilationUnit) JavaCore.create(javaFile);
		region.add(cu);
		IResource[] resources = JavaCore.getGeneratedResources(region, false);
		ASTParser parser = ASTParser.newParser(AST.JLS4);
		parser.setProject(cu.getJavaProject());
		parser.setIgnoreMethodBodies(false);
		IType[] allTypes = getAllTypes(cu);
		IBinding[] bindings = parser.createBindings(allTypes, null);
		for (IResource resource : resources) {
			if (resource instanceof IFile) {
				IFile file = (IFile) resource;
				if ("class".equals(file.getFileExtension())) {
					try {
						final InputStream inputStream = file.getContents();
						try {
							ClassReader reader = new ClassReader(inputStream);
							int version = getClassFileVersion(reader);
							if (version >= Opcodes.V1_5) {
								ClassWriter writer = new ClassWriter(getClassWriterFlags(version));
								String binaryName = file.getFullPath().removeFileExtension().lastSegment();
								ITypeBinding typeBinding = findTypeBinding(binaryName, bindings);
								if (typeBinding != null) {
									final NullnessAssertionInserter nullChecker = new NullnessAssertionInserter(writer, typeBinding,
											finder, fieldCache);
									reader.accept(nullChecker, 0);
									if (nullChecker.isCheckInserted()) {
										ByteArrayInputStream newContent = new ByteArrayInputStream(writer.toByteArray());
										file.setContents(newContent, IResource.NONE, null);
									}
								}
							}
						} finally {
							inputStream.close();
						}
					} catch (CoreException e) {
						// TODO reasonable exception handling
						throw new RuntimeException(e);
					} catch (IOException e) {
						// TODO reasonable exception handling
						throw new RuntimeException(e);
					}
				}
			}
		}
	}

	private IType[] getAllTypes(ICompilationUnit compilationUnit) throws JavaModelException {
		List<IType> result = new ArrayList<IType>(4);
		for (IType type : compilationUnit.getTypes()) {
			result.add(type);
			addAllMemberTypes(type, result);
		}
		return result.toArray(new IType[result.size()]);
	}

	private void addAllMemberTypes(IMember container, List<IType> result) throws JavaModelException {
		for (IJavaElement child : container.getChildren()) {
			if (child instanceof IMember) {
				if (child instanceof IType) {
					result.add((IType) child);
				}
				addAllMemberTypes((IMember) child, result);
			}
		}
	}

	private ITypeBinding findTypeBinding(String binaryClassName, IBinding[] bindings) {
		for (IBinding binding : bindings) {
			if (binding instanceof ITypeBinding) {
				ITypeBinding typeBinding = (ITypeBinding) binding;
				String candidate = typeBinding.getBinaryName();
				if (candidate == null) {
					IType javaType = (IType) typeBinding.getJavaElement();
					candidate = javaType.getTypeQualifiedName('$');
				}
				if (candidate != null) {
					if (candidate.length() == binaryClassName.length()) {
						if (candidate.equals(binaryClassName)) {
							return typeBinding;
						}
					} else if (candidate.endsWith(binaryClassName)
							&& candidate.charAt(candidate.length() - binaryClassName.length() - 1) == '.') {
						return typeBinding;
					}
				}
			}
		}
		return null;
	}

	private int getClassFileVersion(ClassReader reader) {
		// TODO get this from the Java project
		final int[] classfileVersion = new int[1];
		reader.accept(new EmptyVisitor() {
			@Override
			public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
				classfileVersion[0] = version;
			}
		}, 0);

		return classfileVersion[0];
	}

	private int getClassWriterFlags(int version) {
		return version >= Opcodes.V1_6 && version != Opcodes.V1_1 ? ClassWriter.COMPUTE_FRAMES : ClassWriter.COMPUTE_MAXS;
	}

}
