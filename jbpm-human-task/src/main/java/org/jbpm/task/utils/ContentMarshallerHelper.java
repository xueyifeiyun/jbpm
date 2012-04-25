/*
 * Copyright 2012 JBoss by Red Hat.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jbpm.task.utils;

import java.io.*;
import java.util.Map;
import java.util.logging.Level;
import org.drools.marshalling.ObjectMarshallingStrategy;
import org.drools.marshalling.ObjectMarshallingStrategy.Context;
import org.drools.runtime.Environment;
import org.drools.runtime.EnvironmentName;
import org.jbpm.process.workitem.wsht.SyncWSHumanTaskHandler;
import org.jbpm.task.AccessType;
import org.jbpm.task.service.ContentData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ContentMarshallerHelper {

    private static final Logger logger = LoggerFactory.getLogger(SyncWSHumanTaskHandler.class);
    
    
    
    public static ContentData marshal(Object o, ContentMarshallerContext marshallerContext, Environment env) {
        ObjectMarshallingStrategy[] strats = (ObjectMarshallingStrategy[]) env.get(EnvironmentName.OBJECT_MARSHALLING_STRATEGIES);
        ContentData content = null;
        for (ObjectMarshallingStrategy strat : strats) {
            //Use the first strategy that accept the Object based on the order of the provided strategies
            if (strat.accept(o)) {
                try {
                    Context context = strat.createContext();
                    marshallerContext.strategyContext.put(strat, context);    
                    byte[] marshalled = strat.marshal(context, null, o);

                    content = new ContentData();
                    content.setContent(marshalled);
                    // A map by default should be serialized
                    content.setType(strat.getClass().getCanonicalName());
                    content.setAccessType(AccessType.Inline);
                    return content;
                } catch (IOException e) {
                    logger.error(e.getMessage(), e);
                }
            }
        }

        return content;
    }

    public static Object unmarshall(String type, byte[] content, ContentMarshallerContext marshallerContext, Environment env) {

        ObjectMarshallingStrategy[] strats = (ObjectMarshallingStrategy[]) env.get(EnvironmentName.OBJECT_MARSHALLING_STRATEGIES);
        Object data = null;
        ObjectMarshallingStrategy selectedStrat = null;
        for(ObjectMarshallingStrategy strat : strats){
            if(strat.getClass().getCanonicalName().equals(type)){
             selectedStrat = strat;
            }
        }
        Context context = marshallerContext.strategyContext.get(selectedStrat);
        try {
            data = selectedStrat.unmarshal(context, null, content, ContentMarshallerHelper.class.getClassLoader());
        } catch (IOException ex) {
            logger.error(ex.getMessage(), ex);
        } catch (ClassNotFoundException ex) {
            logger.error(ex.getMessage(), ex);
        }

        return data;

    }
}