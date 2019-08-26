package org.ganaga.whereclause;

import java.util.HashMap;
import java.util.Map;


import org.junit.Assert;
import org.junit.Test;


public class WhereClauseTest
{

	private static Map<String,Object> getTestCase1()
	{
		Map<String,Object> result = new HashMap<String,Object>();
		result.put("a", 1);
		result.put("b", 2.0);
		result.put("c", "foo");
		result.put("STATE_ABBR", "UT");
		result.put("STATE_NAME", "California");
		result.put("35-f7", "Street");
		result.put("testNull", null);
		result.put("testNullS", "null");
		result.put("codeiris", "75012");
		result.put("testdollar", "abc$");
		return result;
	}
	
	@Test
	public void testEqual()
	{
				
		try
		{
			Assert.assertTrue(new WhereClause("a=1").accepts(getTestCase1()));
			Assert.assertTrue(new WhereClause("a =1").accepts(getTestCase1()));
			Assert.assertTrue(new WhereClause("a>0").accepts(getTestCase1()));
			Assert.assertTrue(new WhereClause("b=2").accepts(getTestCase1()));
			Assert.assertFalse(new WhereClause("b=2.1").accepts(getTestCase1()));
			Assert.assertFalse(new WhereClause("a=2").accepts(getTestCase1()));
			Assert.assertTrue(new WhereClause("c=foo").accepts(getTestCase1()));
			Assert.assertFalse(new WhereClause("c=bar").accepts(getTestCase1()));
			Assert.assertTrue(new WhereClause("a in (1,2)").accepts(getTestCase1()));
			Assert.assertFalse(new WhereClause("a in (2,3)").accepts(getTestCase1()));
			Assert.assertTrue(new WhereClause("b>1").accepts(getTestCase1()));
			Assert.assertTrue(new WhereClause("b>1.2").accepts(getTestCase1()));
			Assert.assertTrue(new WhereClause("a=1 and b=2").accepts(getTestCase1()));
			Assert.assertFalse(new WhereClause("a=1 and b=3").accepts(getTestCase1()));
			Assert.assertTrue(new WhereClause("a=1 or b=3").accepts(getTestCase1()));
			Assert.assertTrue(new WhereClause("a=2 or c='foo'").accepts(getTestCase1()));
			Assert.assertTrue(new WhereClause("STATE_ABBR in ('AK','AZ','CA','CO','HI','ID','MT','NM','NV','OR','UT','WA','WY')").accepts(getTestCase1()));
			Assert.assertTrue(new WhereClause("a=1 and (b=2 or c='foo')").accepts(getTestCase1()));
			Assert.assertTrue(new WhereClause("(a=1 and (b=2 or c='foo'))").accepts(getTestCase1()));
			Assert.assertTrue(new WhereClause("c='foo' AND ((a >= 0.5 AND a <= 1.5) OR (b > 1.5 AND b <= 2.5))").accepts(getTestCase1()));
			Assert.assertTrue(new WhereClause("STATE_NAME =California").accepts(getTestCase1()));
			Assert.assertTrue(new WhereClause("STATE_NAME IN(California)").accepts(getTestCase1()));
			Assert.assertTrue(new WhereClause("STATE_NAME IN(Calia, California)").accepts(getTestCase1()));
			Assert.assertFalse(new WhereClause("STATE_NAME in(Calia)").accepts(getTestCase1()));
			Assert.assertTrue(new WhereClause("STATE_NAME not in(Calia)").accepts(getTestCase1()));
			Assert.assertTrue(new WhereClause("STATE_NAME not in(Calia, abc)").accepts(getTestCase1()));
			Assert.assertFalse(new WhereClause("STATE_NAME not in(Calia, California)").accepts(getTestCase1()));
			Assert.assertFalse(new WhereClause("STATE_NAME NOT IN(Calia, California)").accepts(getTestCase1()));
			Assert.assertFalse(new WhereClause("STATE_NAME NOT IN (Calia, California)").accepts(getTestCase1()));
			Assert.assertFalse(new WhereClause("STATE_NAME not in(California)").accepts(getTestCase1()));
			Assert.assertTrue(new WhereClause("STATE_NAME like 'Cali%'").accepts(getTestCase1()));
			Assert.assertTrue(new WhereClause("STATE_NAME like '%fornia'").accepts(getTestCase1()));
			Assert.assertTrue(new WhereClause("STATE_NAME like 'C%nia'").accepts(getTestCase1()));
			Assert.assertTrue(new WhereClause("STATE_NAME like 'C%li%nia'").accepts(getTestCase1()));
			Assert.assertFalse(new WhereClause("STATE_NAME like 'C%li%n'").accepts(getTestCase1()));
			Assert.assertFalse(new WhereClause("STATE_NAME not like 'Cali%'").accepts(getTestCase1()));
			Assert.assertTrue(new WhereClause("STATE_NAME not like 'Ut%'").accepts(getTestCase1()));
			Assert.assertTrue(new WhereClause("STATE_NAME not like '%ah'").accepts(getTestCase1()));
			Assert.assertTrue(new WhereClause("35-f7 NOT IN ('District','HouseNumber')'").accepts(getTestCase1()));
			Assert.assertFalse(new WhereClause("35-f7 <> 'Street'").accepts(getTestCase1()));
			Assert.assertTrue(new WhereClause("35-f7 != 'District'").accepts(getTestCase1()));
			Assert.assertTrue(new WhereClause("a != 3").accepts(getTestCase1()));
			Assert.assertTrue(new WhereClause("b<>1.0").accepts(getTestCase1()));
			Assert.assertTrue(new WhereClause("testNull is null").accepts(getTestCase1()));
			Assert.assertFalse(new WhereClause("testNull is not null").accepts(getTestCase1()));
			Assert.assertTrue(new WhereClause("testNullS IS NULL").accepts(getTestCase1()));
			Assert.assertFalse(new WhereClause("testNullS is not null").accepts(getTestCase1()));
			Assert.assertTrue(new WhereClause("testNull = null").accepts(getTestCase1()));
			Assert.assertTrue(new WhereClause("codeiris = 75012").accepts(getTestCase1()));
			
		} catch (Exception e)
		{
			e.printStackTrace();
			Assert.fail(e.toString());
		}
		
	}
	
	@Test
	public void testSubExpressions()
	{
		try
		{
			Assert.assertTrue(new WhereClause("a=1 and (b>0 or c='foo')").accepts(getTestCase1()));
			Assert.assertTrue(!new WhereClause("a=1 and (b=0 and c='foo')").accepts(getTestCase1()));
			Assert.assertTrue(new WhereClause("a=2 or (b>0 and c='foo')").accepts(getTestCase1()));
			Assert.assertTrue(!new WhereClause("a=2 or (b<0 and c='foo')").accepts(getTestCase1()));
			Assert.assertTrue(new WhereClause("a=2 or (b=0 and c='foo') or STATE_NAME in(California)").accepts(getTestCase1()));
			Assert.assertTrue(new WhereClause("a=2 or (b=0 and STATE_NAME in('California')) or c='foo'").accepts(getTestCase1()));
			Assert.assertTrue(!new WhereClause("a=2 or (b=0 and STATE_NAME in(California)) and c='foo'").accepts(getTestCase1()));
			Assert.assertTrue(new WhereClause("a=2 or (b=0 and (STATE_NAME in(California) and STATE_ABBR='UT')) or c='foo'").accepts(getTestCase1()));
			Assert.assertTrue(new WhereClause("STATE_NAME in ('California', 'California')").accepts(getTestCase1()));
			Assert.assertTrue(new WhereClause("(STATE_NAME in ('California', 'California'))").accepts(getTestCase1()));
		} catch (Exception e)
		{
			e.printStackTrace();
			Assert.fail(e.toString());
		}
	}
	
	@Test
	public void testFunctions()
	{
		try
		{
			Assert.assertTrue(new WhereClause("upper(c)='FOO'").accepts(getTestCase1()));
		} catch (Exception e)
		{
			e.printStackTrace();
			Assert.fail(e.toString());
		}
	}
}
