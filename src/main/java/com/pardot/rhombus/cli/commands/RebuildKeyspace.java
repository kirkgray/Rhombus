package com.pardot.rhombus.cli.commands;

import com.pardot.rhombus.cobject.CKeyspaceDefinition;
import com.pardot.rhombus.util.JsonUtil;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;

import java.io.IOException;

/**
 * Rob Righter
 * Date: 8/17/13
 */
public class RebuildKeyspace  extends RcliWithCassandraConfig {

    public Options getCommandOptions(){
        Options ret = super.getCommandOptions();
        Option forceRebuild = new Option( "f", "Force destruction and rebuild of keyspace" );
	    Option keyspaceFile = OptionBuilder.withArgName( "filename" )
			    .hasArg()
			    .withDescription("Filename of json keyspace definition")
			    .create( "keyspacefile" );
	    Option keyspaceResource = OptionBuilder.withArgName( "filename" )
			    .hasArg()
			    .withDescription("Filename of json keyspace definition")
			    .create( "keyspaceresource" );
	    ret.addOption(keyspaceFile);
	    ret.addOption(keyspaceResource);
	    ret.addOption(forceRebuild);
        return ret;
    }

    public void executeCommand(CommandLine cl){
        super.executeCommand(cl);

	    if(!(cl.hasOption("keyspacefile") || cl.hasOption("keyspaceresource"))){
		    displayHelpMessageAndExit();
		    return;
	    }

	    String keyspaceFileName = cl.hasOption("keyspacefile") ? cl.getOptionValue("keyspacefile") : cl.getOptionValue("keyspaceresource");
	    //make the keyspace definition
	    CKeyspaceDefinition keyDef = null;
	    try{
		    keyDef = cl.hasOption("keyspacefile") ?
				    JsonUtil.objectFromJsonFile(CKeyspaceDefinition.class, CKeyspaceDefinition.class.getClassLoader(), keyspaceFileName) :
				    JsonUtil.objectFromJsonResource(CKeyspaceDefinition.class,CKeyspaceDefinition.class.getClassLoader(), keyspaceFileName);
	    }
	    catch (IOException e){
		    System.out.println("Could not parse keyspace file "+keyspaceFileName);
		    System.exit(1);
	    }

	    if(keyDef == null){
		    System.out.println("Could not parse keyspace file "+keyspaceFileName);
		    System.exit(1);
	    }

	    this.keyspaceDefinition = keyDef;
        //now rebuild the keyspace
        try{

            getConnectionManager().buildKeyspace(keyspaceDefinition,cl.hasOption("f"));
            //looks like a success. Lets go ahead and return as such
            System.exit(0);
        }
        catch (Exception e){
            System.out.println("Error encountered while attempting to rebuild the keyspace");
        }

    }
}
