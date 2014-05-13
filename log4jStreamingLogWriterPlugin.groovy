import com.dtolabs.rundeck.plugins.logging.StreamingLogWriterPlugin;
import com.dtolabs.rundeck.core.logging.LogEvent;
import com.dtolabs.rundeck.core.logging.LogLevel;
import org.apache.log4j.Logger
import org.apache.log4j.MDC

/**
 * This example is a minimal streaming log writer plugin for Rundeck
 */
rundeckPlugin(StreamingLogWriterPlugin){
    configuration{
        loggername="com.test.log.stream"
        loggername required:true, description: "Name of logger"
        //if true, log only output from jobs, and not adhoc commands
        jobsOnly=true
        //if true, log output lines, otherwise skip 
        includeLogs=true
        //if true, log when a new node is seen for this execution
        logNewNode=true
    }

    //set log4j MDC with the map data
    def setMDC={map->
        map.each(MDC.&put)
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
     * The "open" closure is called to open the stream for writing events.
     * It is passed two map arguments, the execution data, and the plugin configuration data.
     *
     * It should return a Map containing the stream context, which will be passed back for later
     * calls to the "addEvent" closure.
     */
    open { Map execution, Map config ->
        def ctx=[execution:execution,logger:logger,nodeset:[],config:config]
        //in this example we open a file output stream to store data in JSON format.
        Logger logger = Logger.getLogger(config.loggername)
        if(!config.jobsOnly || execution.id){
            logInfo(execution+['event':'start'],logger,"START: "+execution)
        }
        
        //return context map for the plugin to reuse later
        ctx
    }

    /**
     * "addEvent" closure is called to append a new event to the stream.  
     * It is passed the Map of stream context created in the "open" closure, and a LogEvent.
     * 
     */
    addEvent { Map context, LogEvent event->
        if(event.eventType in [null,'log']){
            //any event with 'log' (or null since it defaults to log), will be ignored
            if(!context.config.includeLogs){
                return   
            }
        }
        if(context.config.jobsOnly && !context.execution.id){
            return
        }
        def newnode=null
        if(event.eventType=='nodebegin'){
            if(event.metadata.node){
                if(!context.nodeset.contains(event.metadata.node)){
                    context.nodeset<<event.metadata.node
                    newnode=event.metadata.node
                }
            }
        }
        def data=[loglevel:event.loglevel.toString(), eventType:event.eventType]
        if(newnode && context.config.logNewNode){
            logInfo(data+event.metadata+['event':'node'],logger,"NODE: "+newnode)
        }
        if(event.eventType in [null,'log'] && context.config.includeLogs){
            logInfo(data+event.metadata+['event':'log'],logger,event.message)
        }
    }
    close { Map context->
        if(context.config.jobsOnly && !context.execution.id){
            return
        }
        
        logInfo(context.subMap(['execution'])+['event':'end'],logger,"END: "+(context.execution))
    }
}