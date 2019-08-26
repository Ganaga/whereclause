package org.ganaga.whereclause;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WhereClause implements IWhereClause {

	private boolean _isCompiled = false;
	private String _whereClause;
	private List<IWhereClause> _blocks;
	private List<String> _links;

	public Map<String, String> aliases = null;

	// order in here is IMPORTANT because we don't want to find '>' or '=' in '>='
	private static Pattern blockPattern3parts = Pattern
			.compile("(^.+?)(<=|>=|<>|!=|<|>|=|not in| in|not like| like)(.+$)", Pattern.CASE_INSENSITIVE);
	private static Pattern blockPattern2parts = Pattern.compile("(^.+?)(is not null|is null)$",
			Pattern.CASE_INSENSITIVE);

	public WhereClause(String whereClause) {
		this._whereClause = whereClause;
	}

	public WhereClause compile() throws WhereClauseException {
		try {
			_blocks = new ArrayList<IWhereClause>();
			_links = new ArrayList<String>();

			String tmp = _whereClause.replace('\n', ' ');
			int idx = 0;

			do {
				String link = null;

				tmp = tmp.trim();
				String subWhere = null;
				if (tmp.startsWith("(")) {
					idx = searchOutOfQuotes(tmp.toLowerCase().substring(1), ")");
					if (idx == -1)
						throw new WhereClauseException(this._whereClause);
					subWhere = tmp.substring(1, idx + 1);
					tmp = "SUBWHERE " + tmp.substring(idx + 2, tmp.length());
				}

				int andIdx = searchOutOfQuotes(tmp.toLowerCase(), " and");
				int orIdx = searchOutOfQuotes(tmp.toLowerCase(), " or");
				if (andIdx > 0 && (andIdx < orIdx || orIdx < 0)) {
					idx = andIdx;
					link = "and";
				} else if (orIdx > 0) {
					idx = orIdx;
					link = "or";
				} else
					idx = -1;

				IWhereClause block = null;
				if (subWhere == null) {
					subWhere = idx >= 0 ? tmp.substring(0, idx) : tmp;
					block = compile(subWhere);
				} else {
					block = new WhereClause(subWhere).compile();
				}
				_blocks.add(block);
				if (link != null)
					_links.add(link);

				if (idx < 0)
					break;
				else
					tmp = tmp.substring(idx + link.length() + 1);

			} while (true);

			_isCompiled = true;
			return this;
		} catch (Exception e) {
			throw new WhereClauseException(this._whereClause, e);
		}
	}

	public List<String> getFields() throws WhereClauseException {
		if (!this._isCompiled)
			this.compile();
		List<String> result = new ArrayList<String>();
		for (IWhereClause block : _blocks) {
			result.addAll(block.getFields());
		}
		return result;
	}

	public boolean hasField(String fieldName) throws WhereClauseException {
		if (!this._isCompiled)
			this.compile();
		for (String name : getFields()) {
			if (name.equals(fieldName))
				return true;
		}
		return false;
	}

	private WhereClauseBlock compile(String subWhereClause) throws WhereClauseException {
		String field = null;
		String operator = null;
		String value = null;
		// String[] split = null;

		if (subWhereClause == null || subWhereClause.trim().equals("1=1"))
			return new WhereClauseBlock.TrueWhereClauseBlock();

		Matcher match3parts = blockPattern3parts.matcher(subWhereClause);
		Matcher match2parts = blockPattern2parts.matcher(subWhereClause);

		if (match3parts.matches() && match3parts.groupCount() == 3) {
			field = match3parts.group(1).trim();
			operator = match3parts.group(2).trim().toLowerCase();
			value = match3parts.group(3).trim();
		} else if (match2parts.matches() && match2parts.groupCount() == 2) {
			field = match2parts.group(1).trim();
			operator = match2parts.group(2).trim().toLowerCase();
			value = "null";
		} else
			throw new WhereClauseException("Invalid where clause " + subWhereClause);

		WhereClauseBlock result = new WhereClauseBlock(field, operator, value);
		result.compile();
		return result;
	}

	public boolean accepts(Map<String, Object> attributes) throws WhereClauseException {
		return accepts(attributes, aliases);
	}

	public boolean accepts(Map<String, Object> attributes, Map<String, String> inAliases) throws WhereClauseException {
		if (!_isCompiled)
			this.compile();

		int idx = 0;
		boolean ok = false;
		for (IWhereClause block : _blocks) {
			ok = block.accepts(attributes, inAliases);
			if (_links.size() <= idx) {
				return ok;
			} else if (_links.get(idx).equals("and")) {
				if (!ok)
					return false;
			} else if (_links.get(idx).equals("or")) {
				if (ok)
					return true;
			}
			idx++;
		}
		return false;
	}

	public List<IWhereClause> getBlocks() {
		return this._blocks;
	}

	public String toString() {
		return this._whereClause;
	}

	public static int searchOutOfQuotes(String s, String searchString) {
		boolean insideQuotes = false;
		int subblockDepth = 0;
		int parseIdx = 0;
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);

			if (!insideQuotes && subblockDepth == 0 && s.length() > i + parseIdx
					&& s.charAt(i) == searchString.charAt(parseIdx)) {
				if (parseIdx == searchString.length() - 1)
					return i - parseIdx;
				parseIdx++;
			} else if (!insideQuotes && subblockDepth == 0 && s.length() > i + parseIdx
					&& s.charAt(i) == searchString.charAt(0)) {
				if (searchString.length() == 1)
					return i - 1;
				parseIdx = 1;
			} else {
				parseIdx = 0;
			}
			if (c == '\'')
				insideQuotes = !insideQuotes;
			if (c == '(' && !insideQuotes)
				subblockDepth++;
			if (c == ')' && !insideQuotes)
				subblockDepth--;
		}
		return -1;
	}
}
