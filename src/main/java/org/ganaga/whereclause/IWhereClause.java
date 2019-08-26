package org.ganaga.whereclause;

import java.util.List;
import java.util.Map;

public interface IWhereClause {
	public boolean accepts(Map<String, Object> attributes) throws WhereClauseException;

	public boolean accepts(Map<String, Object> attributes, Map<String, String> aliases) throws WhereClauseException;

	public List<String> getFields() throws WhereClauseException;
}
