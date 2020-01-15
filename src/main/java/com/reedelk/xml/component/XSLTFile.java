package com.reedelk.xml.component;

import com.reedelk.runtime.api.annotation.ESBComponent;
import com.reedelk.runtime.api.annotation.Property;
import com.reedelk.runtime.api.annotation.PropertyInfo;
import com.reedelk.runtime.api.component.ProcessorSync;
import com.reedelk.runtime.api.message.FlowContext;
import com.reedelk.runtime.api.message.Message;
import com.reedelk.runtime.api.script.dynamicvalue.DynamicString;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ServiceScope;

@ESBComponent("XSLT File")
@Component(service = XSLTFile.class, scope = ServiceScope.PROTOTYPE)
public class XSLTFile implements ProcessorSync {

    @Property("XSLT File")
    @PropertyInfo("The path and name of the file to be read from the file system.")
    private DynamicString fileName;

    @Override
    public Message apply(Message message, FlowContext flowContext) {
        return null;
    }

    public void setFileName(DynamicString fileName) {
        this.fileName = fileName;
    }
}