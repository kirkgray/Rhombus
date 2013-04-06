package com.pardot.service.analytics;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pardot.service.analytics.helpers.TestHelpers;
import com.pardot.service.tools.cobject.CField;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import com.pardot.service.tools.cobject.CDefinition;
import org.apache.commons.io.*;
import java.io.FileInputStream;
import java.util.ArrayList;

/**
 * Pardot, An ExactTarget Company
 * User: robrighter
 * Date: 4/5/13
 */
public class CDefinitionTest extends TestCase{

	public class Subject extends CDefinition {

		public void testGenerateFields(){
			String json = TestHelpers.readFileToString(this.getClass(),"CFieldsTestData.js");
			assertTrue("Invalid Test File", !json.equals(""));
			try{
				java.net.URL location = Test.class.getProtectionDomain().getCodeSource().getLocation();
				System.out.println(location.getFile());
				ObjectMapper mapper = new ObjectMapper();
				JsonNode j =  mapper.readTree(json);
				ArrayList<CField> result = this.generateFields(j);
				assertTrue("Should have 3 fields in result", result.size() == 3);
				assertTrue("First name should be accountId",result.get(0).name.equals("accountId"));
				assertTrue("First type should be bigint",result.get(0).type == CField.CDataType.BIGINT);
				assertTrue("Second name should be accountId",result.get(1).name.equals("fieldAsTime"));
				assertTrue("Second type should be bigint",result.get(1).type == CField.CDataType.TIMEUUID);
				assertTrue("Third name should be accountId",result.get(2).name.equals("fieldWithInvalidType"));
				assertTrue("Third type should be bigint",result.get(2).type == CField.CDataType.VARCHAR);
			}
			catch(Exception e){
				assertTrue(e.toString(), false);
			}
		}

	}

	/**
	 * Create the test case
	 *
	 * @param testName name of the test case
	 */
	public CDefinitionTest( String testName )
	{
		super( testName );
	}

	/**
	 * @return the suite of tests being tested
	 */
	public static Test suite()
	{
		return new TestSuite( CDefinitionTest.class );
	}

	/**
	 * Rigourous Test :-)
	 */
	public void testGenerateFields()
	{
		Subject s = new Subject();
		s.testGenerateFields();
	}
}
