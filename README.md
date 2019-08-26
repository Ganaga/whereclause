# whereclause
A simple and fast library used to evaluate where clauses.

Usage:

Map<String, Object> attributes = new HashMap<String, Object>();
attributes.put("population", 10000);
attributes.put("country, "France");
attributes.put("desc, "Lorem ipsum");

assertTrue( new WhereClause("population > 42").accepts(attributes) );
assertTrue( new WhereClause("population > 42 and population < 100001").accepts(attributes) );
assertTrue( new WhereClause("country in ('France')").accepts(attributes) );
assertTrue( new WhereClause("country like ('%ance')").accepts(attributes) );
