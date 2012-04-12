package org.jboss.as.console.spi;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.tools.FileObject;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;

import org.jboss.as.console.spi.client.plugins.SubsystemExtension;

import com.gwtplatform.mvp.client.annotations.NameToken;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static javax.lang.model.SourceVersion.RELEASE_6;

@SupportedSourceVersion(RELEASE_6)
public class ExtensionProcessor extends AbstractProcessor {

    private static final String GINJECTOR_TEMPLATE = "Extension.tmpl";
    private static final String GINJECTOR_FILENAME = "org.jboss.as.console.composite.client.CompositeGinjector";

    private static final String BINDING_TEMPLATE = "CompositeBinding.tmpl";
    private static final String BINDING_FILENAME = "org.jboss.as.console.composite.client.CompositeBinding";

    private static final String SINGLETON_TEMPLATE = "CompositeGinjectorSingleton.tmpl";
    private static final String SINGLETON_FILENAME = "org.jboss.as.console.composite.client.CompositeGinjectorSingleton";

    private static final String BEAN_FACTORY_TEMPLATE = "BeanFactory.tmpl";
    private static final String BEAN_FACTORY_FILENAME = "org.jboss.as.console.composite.client.CompositeBeanFactory";

    private static final String SUBSYSTEM_FILENAME = "org.jboss.as.console.composite.client.SubsystemRegistryImpl";
    private static final String SUBSYSTEM_TEMPLATE = "SubsystemExtensions.tmpl";

    private static final String MODULE_FILENAME = "App.gwt.xml";
    private static final String MODULE_DEV_FILENAME = "App_dev.gwt.xml";
    private static final String MODULE_PACKAGENAME = "org.jboss.as.console.composite";
    private static final String MODULE_TEMPLATE = "App.gwt.xml.tmpl";
    private static final String MODULE_DEV_TEMPLATE = "App_dev.gwt.xml.tmpl";

    private Filer filer;
    private Messager messager;
    private ProcessingEnvironment processingEnv;
    private List<String> discoveredExtensions;
    private Set<String> modules = new LinkedHashSet<String>();
    private List<String> discoveredBeanFactories;
    private List<SubsystemExtension> subsystemDeclararions = new ArrayList<SubsystemExtension>();
    private Set<String> nameTokens = new HashSet<String>();

    @Override
    public void init(ProcessingEnvironment env) {
        this.processingEnv = env;
        this.filer = env.getFiler();
        this.messager = env.getMessager();
        this.discoveredExtensions = new ArrayList<String>();
        this.discoveredBeanFactories = new ArrayList<String>();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> types = new HashSet<String>();
        types.add(GinExtension.class.getName());
        types.add(BeanFactoryExtension.class.getName());
        types.add(Subsystem.class.getName());
        types.add(NameToken.class.getName());
        return types;
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

        if(!roundEnv.processingOver()) {
            System.out.println("Begin Components discovery ...");

            Set<? extends Element> extensionElements = roundEnv.getElementsAnnotatedWith(GinExtension.class);

            for (Element element: extensionElements)
            {
                handleGinExtensionElement(element);
            }

            System.out.println("Begin BeanFactory discovery ...");

            Set<? extends Element> beanFactoryElements = roundEnv.getElementsAnnotatedWith(BeanFactoryExtension.class);

            for (Element element: beanFactoryElements)
            {
                handleBeanFactoryElement(element);
            }

            System.out.println("Begin Subsystem discovery ...");

            Set<? extends Element> subsystemElements = roundEnv.getElementsAnnotatedWith(Subsystem.class);

            for (Element element: subsystemElements)
            {
                handleSubsystemElement(element);
            }
        }

        if (roundEnv.processingOver())
        {
            try {
                // generate the actual implementation
                writeFiles();

            } catch (Exception e) {
                e.printStackTrace();
            }

            System.out.println("Component and BeanFactory discovery completed.");
        }

        return true;
    }

    private void handleGinExtensionElement(Element element) {
        List<? extends AnnotationMirror> annotationMirrors = element.getAnnotationMirrors();

        for (AnnotationMirror mirror: annotationMirrors)
        {
            final String annotationType = mirror.getAnnotationType().toString();

            if ( annotationType.equals(GinExtension.class.getName()) )
            {
                GinExtension comps  = element.getAnnotation(GinExtension.class);
                final String module = comps.value();
                if (module != null && module.length() > 0) {
                    modules.add(module);
                }

                PackageElement packageElement = processingEnv.getElementUtils().getPackageOf(element);
                String fqn = packageElement.getQualifiedName().toString()+"."+
                        element.getSimpleName().toString();
                System.out.println("Components: " + fqn);
                discoveredExtensions.add(fqn);
            }
        }
    }

    private void handleBeanFactoryElement(Element element) {
        List<? extends AnnotationMirror> annotationMirrors = element.getAnnotationMirrors();

        for (AnnotationMirror mirror: annotationMirrors)
        {
            final String annotationType = mirror.getAnnotationType().toString();

            if ( annotationType.equals(BeanFactoryExtension.class.getName()) )
            {
                BeanFactoryExtension factory  = element.getAnnotation(BeanFactoryExtension.class);

                PackageElement packageElement = processingEnv.getElementUtils().getPackageOf(element);
                String fqn = packageElement.getQualifiedName().toString()+"."+
                        element.getSimpleName().toString();
                System.out.println("Factory: " + fqn);
                discoveredBeanFactories.add(fqn);
            }
        }
    }

    private void handleSubsystemElement(Element element) {
        List<? extends AnnotationMirror> annotationMirrors = element.getAnnotationMirrors();

        for (AnnotationMirror mirror: annotationMirrors)
        {
            final String annotationType = mirror.getAnnotationType().toString();

            if ( annotationType.equals(Subsystem.class.getName()) )
            {
                NameToken nameToken = element.getAnnotation(NameToken.class);
                Subsystem subsystem = element.getAnnotation(Subsystem.class);

                if(nameToken!=null)   {
                    System.out.println("Subsystem: " + subsystem.name() +" -> "+nameToken.value());

                    SubsystemExtension declared = new SubsystemExtension(
                            subsystem.name(), nameToken.value(),
                            subsystem.group(), subsystem.key()
                    );

                    subsystemDeclararions.add(declared);
                    if (!nameTokens.add(nameToken.value())) {
                        throw new RuntimeException("Duplicate name token '" + nameToken.value() + "' declared on '"
                                + element.asType());
                    }
                }
            }
        }
    }

    private void writeFiles() {
        writeGinjectorFile();
        writeBindingFile();
        writeGinjectorSingletonFile();
        writeBeanFactoryFile();
        writeSubsystemFile();
        writeModuleFile();
        writeDevModuleFile();
    }

    private void writeGinjectorFile() {

        try
        {
            Map<String, Object> model = new HashMap<String, Object>();
            model.put("extensions", discoveredExtensions);

            JavaFileObject sourceFile = filer.createSourceFile(GINJECTOR_FILENAME);
            OutputStream output = sourceFile.openOutputStream();
            new TemplateProcessor().process(GINJECTOR_TEMPLATE, model, output);
            output.flush();
            output.close();
        }
        catch (IOException e)
        {
            throw new RuntimeException("Failed to create Composite file", e);
        }
    }

    private void writeBindingFile() {

        try
        {
            Map<String, Object> model = new HashMap<String, Object>();
            model.put("extensions", discoveredBeanFactories);

            JavaFileObject sourceFile = filer.createSourceFile(BINDING_FILENAME);
            OutputStream output = sourceFile.openOutputStream();
            new TemplateProcessor().process(BINDING_TEMPLATE, model, output);
            output.flush();
            output.close();
        }
        catch (IOException e)
        {
            throw new RuntimeException("Failed to create CompositeBinding file", e);
        }
    }

    private void writeGinjectorSingletonFile() {

        try
        {
            JavaFileObject sourceFile = filer.createSourceFile(SINGLETON_FILENAME);
            OutputStream output = sourceFile.openOutputStream();
            new TemplateProcessor().process(SINGLETON_TEMPLATE, Collections.<String, Object>emptyMap(), output);
            output.flush();
            output.close();
        }
        catch (IOException e)
        {
            throw new RuntimeException("Failed to create CompositeBinding file", e);
        }
    }

    private void writeBeanFactoryFile() {

        try
        {
            Map<String, Object> model = new HashMap<String, Object>();
            model.put("extensions", discoveredBeanFactories);

            JavaFileObject sourceFile = filer.createSourceFile(BEAN_FACTORY_FILENAME);
            OutputStream output = sourceFile.openOutputStream();
            new TemplateProcessor().process(BEAN_FACTORY_TEMPLATE, model, output);
            output.flush();
            output.close();
        }
        catch (IOException e)
        {
            throw new RuntimeException("Failed to create BeanFactory file", e);
        }
    }

    private void writeSubsystemFile() {

        try
        {
            Map<String, Object> model = new HashMap<String, Object>();
            model.put("subsystemExtensions", subsystemDeclararions);

            JavaFileObject sourceFile = filer.createSourceFile(SUBSYSTEM_FILENAME);
            OutputStream output = sourceFile.openOutputStream();
            new TemplateProcessor().process(SUBSYSTEM_TEMPLATE, model, output);
            output.flush();
            output.close();
        }
        catch (IOException e)
        {
            throw new RuntimeException("Failed to create file", e);
        }
    }

    private void writeModuleFile() {

        try
        {
            Map<String, Object> model = new HashMap<String, Object>();
            model.put("modules", modules);

            FileObject sourceFile = filer.createResource(StandardLocation.SOURCE_OUTPUT, MODULE_PACKAGENAME,
                    MODULE_FILENAME);
            OutputStream output = sourceFile.openOutputStream();
            new TemplateProcessor().process(MODULE_TEMPLATE, model, output);
            output.flush();
            output.close();
        }
        catch (IOException e)
        {
            throw new RuntimeException("Failed to create file", e);
        }
    }

    private void writeDevModuleFile() {

        try
        {
            Map<String, Object> model = new HashMap<String, Object>();
            model.put("modules", modules);

            FileObject sourceFile = filer.createResource(StandardLocation.SOURCE_OUTPUT, MODULE_PACKAGENAME,
                    MODULE_DEV_FILENAME);
            OutputStream output = sourceFile.openOutputStream();
            new TemplateProcessor().process(MODULE_DEV_TEMPLATE, model, output);
            output.flush();
            output.close();
        }
        catch (IOException e)
        {
            throw new RuntimeException("Failed to create file", e);
        }
    }
}

