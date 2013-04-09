/*
 * Copyright 2010-2013 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.jet.lang.resolve.lazy;

import com.google.common.base.Predicate;
import com.intellij.openapi.util.io.FileUtil;
import org.jetbrains.jet.JetTestUtils;
import org.jetbrains.jet.checkers.AbstractJetDiagnosticsTest;
import org.jetbrains.jet.lang.descriptors.ModuleDescriptor;
import org.jetbrains.jet.lang.descriptors.PackageViewDescriptor;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.name.FqNameUnsafe;
import org.jetbrains.jet.lang.resolve.name.Name;

import java.io.File;
import java.util.Collection;
import java.util.Set;

import static org.jetbrains.jet.test.util.NamespaceComparator.RECURSIVE;
import static org.jetbrains.jet.test.util.NamespaceComparator.compareNamespaces;

public abstract class AbstractLazyResolveDiagnosticsTest extends AbstractJetDiagnosticsTest {

    public static final File TEST_DATA_DIR = new File("compiler/testData/diagnostics/tests");

    @Override
    protected void analyzeAndCheck(File testDataFile, String expectedText, TestEnvironment testEnvironment) {
        Collection<JetFile> jetFiles = testEnvironment.getCoreEnvironment().getSourceFiles();
        ModuleDescriptor lazyModule = LazyResolveTestUtil.resolveLazily(jetFiles, testEnvironment.getCoreEnvironment());
        ModuleDescriptor eagerModule = LazyResolveTestUtil.resolveEagerly(jetFiles, testEnvironment.getCoreEnvironment());

        String path = JetTestUtils.getFilePath(new File(FileUtil.getRelativePath(TEST_DATA_DIR, testDataFile)));
        PackageViewDescriptor expected = DescriptorUtils.getRootPackage(eagerModule.getSubModules().iterator().next());
        PackageViewDescriptor actual = DescriptorUtils.getRootPackage(lazyModule.getSubModules().iterator().next());

        String txtFileRelativePath = path.replaceAll("\\.kt$|\\.ktscript", ".txt");
        File txtFile = new File("compiler/testData/lazyResolve/diagnostics/" + txtFileRelativePath);

        // Only recurse into those namespaces mentioned in the files
        // Otherwise we'll be examining the whole JDK
        final Set<Name> names = LazyResolveTestUtil.getTopLevelPackagesFromFileList(jetFiles);
        compareNamespaces(
                expected, actual,
                RECURSIVE.filterRecursion(new Predicate<FqNameUnsafe>() {
                    @Override
                    public boolean apply(FqNameUnsafe fqName) {
                        if (fqName.isRoot()) return true;
                        if (fqName.parent().isRoot()) {
                            return names.contains(fqName.shortName());
                        }
                        return true;
                    }
                }),
                txtFile);
    }
}
