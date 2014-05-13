import com.dtolabs.rundeck.plugins.logging.StreamingLogWriterPlugin;
import com.dtolabs.rundeck.core.logging.LogEvent;
import com.dtolabs.rundeck.core.logging.LogLevel;
import org.apache.log4j.Logger
import org.apache.log4j.MDC
/*
 * Copyright 2014 SimplifyOps, Inc. (http://simplifyops.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

/**
 * A streaming log writer plugin which outputs events to log4j.
 * This plugin only outputs these messages:
 * * job start
 * * job end
 * * node step started on a new node.
 */
rundeckPlugin(StreamingLogWriterPlugin){
    configuration{
        loggername="com.test.log.stream"
        loggername required:true, description: "Name of log4j logger to use, default: com.test.log.stream"
        
        jobsOnly=true
        jobsOnly required:false, description: "if true, log only output from jobs, and not adhoc commands"
        
        includeLogs=true
        includeLogs required:false, description: "if true, log output lines, otherwise skip"
        
        logNewNode=true
        logNewNode required:false, description: "if true, log when a new node is seen for this execution"
    }

    //set log4j MDC with the map data
    def setMDC={map->
        map.findAll{it.value}.each(MDC.&put)
    }
    //remove map keys from MDC
    def clearMDC={map->
        map.keySet().each(MDC.&remove)
    }
    //log at info level the message, using the logger and given MDC map data
    def logInfo={map,logger,msg->
        setMDC(map)
        logger.info(msg)
        clearMDC(map)
    }

    /**
     * Open is called when execution is started.  log start event and set up context
     */
    open { Map execution, Map config ->
        //in this example we open a file output stream to store data in JSON format.
        Logger logger = Logger.getLogger(config.loggername)
        def ctx=[execution:execution,logger:logger,nodeset:[],config:config]
        if(!config.jobsOnly || execution.id){
            logInfo(execution+['event':'start'],logger,"START")
        }
        
        //return context map for the plugin to reuse later
        return ctx
    }

    /**
     * new log event
     */
    addEvent { Map context, LogEvent event->

        //any event with 'log' (or null since it defaults to log), will be ignored
        if((event.eventType in [null,'log']) && !context.config.includeLogs){
            return   
        }
        //only interested in events for job executions
        if(context.config.jobsOnly && !context.execution.id){
            return
        }

        //determine if a new node is being run on
        def newnode=null
        if(event.eventType=='nodebegin' && event.metadata.node && !context.nodeset.contains(event.metadata.node)){
            context.nodeset<<event.metadata.node
            newnode=event.metadata.node
        }

        def data=[loglevel:event.loglevel.toString(), eventType:event.eventType]

        //log new node event
        if(newnode && context.config.logNewNode){
            logInfo(context.execution+data+event.metadata+['event':'node',nodename:newnode],context.logger,newnode)
        }

        //if includeLogs, log the message
        if((event.eventType in [null,'log']) && context.config.includeLogs){
            logInfo(context.execution+data+event.metadata+['event':'log'],context.logger,event.message)
        }
    }

    /**
     * close: log the execution end event.
     */
    close { Map context->
        
        if(!context.config.jobsOnly || context.execution.id){
            logInfo(context.execution+['event':'end'],context.logger,"END")
        }
    }
}