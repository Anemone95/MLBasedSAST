package top.anemone.mlsast.core.joana;

import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.types.MethodReference;
import edu.kit.joana.wala.core.EntrypointFactory;

public class AppEntrypointFactory implements EntrypointFactory {
    @Override
    public Entrypoint create(IMethod method, IClassHierarchy cha) {
        return new AppEntrypoint(method, cha);
    }

    @Override
    public Entrypoint create(MethodReference method, IClassHierarchy cha) {
        return new AppEntrypoint(method, cha);
    }

}
