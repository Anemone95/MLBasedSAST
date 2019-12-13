package top.anemone.mlsast.slice.joana;

import com.ibm.wala.classLoader.ClassLoaderFactory;
import com.ibm.wala.classLoader.ClassLoaderFactoryImpl;
import com.ibm.wala.classLoader.Language;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.util.MonitorUtil;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class AppClassHierarchyFactory {

    /**
     * @return a ClassHierarchy object representing the analysis scope
     */
    public static AppClassHierarchy make(AnalysisScope scope) throws ClassHierarchyException {
        if (scope == null) {
            throw new IllegalArgumentException("null scope");
        }
        return make(scope, new ClassLoaderFactoryImpl(scope.getExclusions()));
    }

    /**
     * @return a ClassHierarchy object representing the analysis scope, where phantom classes are
     * created when superclasses are missing
     */
    public static AppClassHierarchy makeWithPhantom(AnalysisScope scope) throws ClassHierarchyException {
        if (scope == null) {
            throw new IllegalArgumentException("null scope");
        }
        return makeWithPhantom(scope, new ClassLoaderFactoryImpl(scope.getExclusions()));
    }

    /**
     * @return a ClassHierarchy object representing the analysis scope, missing superclasses are
     * replaced by the ClassHierarchy root, i.e. java.lang.Object
     */
    public static AppClassHierarchy makeWithRoot(AnalysisScope scope) throws ClassHierarchyException {
        if (scope == null) {
            throw new IllegalArgumentException("null scope");
        }
        return makeWithRoot(scope, new ClassLoaderFactoryImpl(scope.getExclusions()));
    }

    /**
     * temporarily marking this internal to avoid infinite sleep with randomly chosen
     * IProgressMonitor.
     */
    public static AppClassHierarchy make(AnalysisScope scope, MonitorUtil.IProgressMonitor monitor)
            throws ClassHierarchyException {
        if (scope == null) {
            throw new IllegalArgumentException("null scope");
        }
        return make(scope, new ClassLoaderFactoryImpl(scope.getExclusions()), monitor);
    }

    public static AppClassHierarchy make(AnalysisScope scope, ClassLoaderFactory factory)
            throws ClassHierarchyException {
        return make(scope, factory, ClassHierarchy.MissingSuperClassHandling.NONE);
    }

    private static AppClassHierarchy make(
            AnalysisScope scope,
            ClassLoaderFactory factory,
            ClassHierarchy.MissingSuperClassHandling superClassHandling)
            throws ClassHierarchyException {
        if (scope == null) {
            throw new IllegalArgumentException("null scope");
        }
        if (factory == null) {
            throw new IllegalArgumentException("null factory");
        }
        return new AppClassHierarchy(scope, factory, null, new ConcurrentHashMap<>(), superClassHandling);
    }

    public static AppClassHierarchy makeWithPhantom(AnalysisScope scope, ClassLoaderFactory factory)
            throws ClassHierarchyException {
        return make(scope, factory, ClassHierarchy.MissingSuperClassHandling.PHANTOM);
    }

    public static AppClassHierarchy makeWithRoot(AnalysisScope scope, ClassLoaderFactory factory)
            throws ClassHierarchyException {
        return make(scope, factory, ClassHierarchy.MissingSuperClassHandling.ROOT);
    }

    /**
     * temporarily marking this internal to avoid infinite sleep with randomly chosen
     * IProgressMonitor.
     */
    public static AppClassHierarchy make(
            AnalysisScope scope, ClassLoaderFactory factory, MonitorUtil.IProgressMonitor monitor)
            throws ClassHierarchyException {
        return new AppClassHierarchy(
                scope,
                factory,
                monitor,
                new ConcurrentHashMap<>(),
                ClassHierarchy.MissingSuperClassHandling.NONE);
    }

    public static AppClassHierarchy make(
            AnalysisScope scope, ClassLoaderFactory factory, Set<Language> languages)
            throws ClassHierarchyException {
        return new AppClassHierarchy(
                scope,
                factory,
                languages,
                null,
                new ConcurrentHashMap<>(),
                ClassHierarchy.MissingSuperClassHandling.NONE);
    }

    public static AppClassHierarchy make(
            AnalysisScope scope, ClassLoaderFactory factory, Language language)
            throws ClassHierarchyException {
        return new AppClassHierarchy(
                scope,
                factory,
                language,
                null,
                new ConcurrentHashMap<>(),
                ClassHierarchy.MissingSuperClassHandling.NONE);
    }

    /**
     * temporarily marking this internal to avoid infinite sleep with randomly chosen
     * IProgressMonitor. TODO: nanny for testgen
     */
    public static AppClassHierarchy make(
            AnalysisScope scope, ClassLoaderFactory factory, Language language, MonitorUtil.IProgressMonitor monitor)
            throws ClassHierarchyException {
        if (factory == null) {
            throw new IllegalArgumentException("null factory");
        }
        return new AppClassHierarchy(
                scope,
                factory,
                language,
                monitor,
                new ConcurrentHashMap<>(),
                ClassHierarchy.MissingSuperClassHandling.NONE);
    }
}
