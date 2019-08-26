package org.ganaga.whereclause;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public class WhereClauseBlock implements IWhereClause {
	private static Pattern doublePattern = Pattern.compile("-?\\d+(\\.\\d+)?(E-?\\d+)?");
	private static Pattern longPattern = Pattern.compile("^-?\\d+$");

	String _field;
	String _operator;
	String _value;
	String _function = null;

	private Object _compiledValue = null;

	WhereClauseBlock(String field, String operator, String value) {
		this._field = field != null ? field.trim() : null;
		this._operator = operator != null ? operator.trim() : null;
		this._value = value != null ? value.trim() : null;
		if (this._field != null) {
			int idx = this._field.indexOf('(');
			if (idx > 0 && this._field.endsWith("")) {
				this._function = this._field.substring(0, idx).toLowerCase();
				this._field = this._field.substring(idx + 1, this._field.length() - 1);
			}
		}
	}

	public WhereClauseBlock compile() {
		if (_value == null)
			_compiledValue = null;
		else {
			_value = _value.trim();
			if (_value.length() < 20 && longPattern.matcher(_value).matches())
				_compiledValue = Long.parseLong(_value);// Integer.parseInt(_value);
			else if (doublePattern.matcher(_value).matches())
				_compiledValue = Double.parseDouble(_value);
			else if (_value.startsWith("(") && _value.endsWith("")) {
				String tmp = _value.substring(1, _value.length() - 1);
				_compiledValue = stringToList(tmp, ',');
			} else {
				if (_value.startsWith("'") && _value.endsWith("'")) {
					String string = _value.substring(1, _value.length() - 1);
					if (string.contains("''"))
						string = string.replaceAll("''", "'");
					_compiledValue = string;
				} else {
					// Assume _value is a string
					_compiledValue = _value;
				}
				if ("like".equalsIgnoreCase(this._operator) || "not like".equalsIgnoreCase(this._operator)) {
					String regex = (_compiledValue + "").toLowerCase().replaceAll("%", ".*").replaceAll("$", "\\$");
					_compiledValue = Pattern.compile(regex);
				}
			}
		}
		return this;
	}

	public boolean accepts(Map<String, Object> attributes) throws WhereClauseException {
		return accepts(attributes, null);
	}

	public boolean accepts(Map<String, Object> attributes, Map<String, String> aliases) throws WhereClauseException {
		Object val = getField(attributes, this._field);
		if (val == null && aliases != null && aliases.get(this._field) != null) {
			val = attributes.get(aliases.get(this._field));
		}
		if (val == null && !"null".equalsIgnoreCase(_value)) {
			// Skip missing field
			return false;
		}
		return accepts(val);
	}

	private Object getField(Map<String, Object> attributes, String field) {
		Object val = attributes.get(field);
		if (val == null) {
			for (String key : attributes.keySet()) {
				if (key.equalsIgnoreCase(field))
					return attributes.get(key);
			}
		}
		return val;
	}

	private boolean accepts(Object val) throws WhereClauseException {
		try {
			Double doubleVal = null;
			if (val instanceof Number)
				doubleVal = ((Number) val).doubleValue();

			String stringVal = val == null ? null : val.toString();
			if (val != null && val instanceof Double && stringVal.endsWith(".0"))
				stringVal = stringVal.substring(0, stringVal.length() - 2);

			if (this._function != null) {
				if ("upper".equals(this._function))
					stringVal = stringVal.toUpperCase();
				if ("lower".equals(this._function))
					stringVal = stringVal.toLowerCase();
			}

			if (this._operator.equals(">")) {
				if (stringVal == null)
					return false;
				if (doubleVal != null)
					return doubleVal > this.getDoubleValue();
				else
					return stringVal.compareTo(this.getStringValue()) > 0;
			} else if (this._operator.equals(">=")) {
				if (stringVal == null)
					return false;
				if (doubleVal != null)
					return doubleVal >= this.getDoubleValue();
				else
					return stringVal.compareTo(this.getStringValue()) >= 0;
			} else if (this._operator.equals("<")) {
				if (stringVal == null)
					return false;
				if (doubleVal != null)
					return doubleVal < this.getDoubleValue();
				else
					return stringVal.compareTo(this.getStringValue()) < 0;
			} else if (this._operator.equals("<=")) {
				if (stringVal == null)
					return false;
				if (doubleVal != null)
					return doubleVal <= this.getDoubleValue();
				else
					return stringVal.compareTo(this.getStringValue()) <= 0;
			} else if (this._operator.equals("=")) {
				if (stringVal == null)
					return "null".equalsIgnoreCase(_value);
				if (doubleVal != null)
					return doubleVal.equals(this.getDoubleValue());
				else
					return stringVal.toLowerCase().trim().equals(this.getStringValue().toLowerCase().trim());
			} else if (this._operator.equals("<>") || this._operator.equals("!=")) {
				if (stringVal == null)
					return !"null".equalsIgnoreCase(_value);
				if (doubleVal != null)
					return !doubleVal.equals(this.getDoubleValue());
				else
					return !stringVal.toLowerCase().equals(this.getStringValue().toLowerCase());
			} else if (this._operator.toLowerCase().equals("is not null")) {
				if ("null".equalsIgnoreCase(this.getStringValue())) {
					return stringVal != null && !"null".equalsIgnoreCase(stringVal);
				}
				throw new WhereClauseException("operator 'is not " + this.getStringValue() + "' not supported");
			} else if (this._operator.toLowerCase().equals("is null")) {
				if ("null".equalsIgnoreCase(this.getStringValue())) {
					return stringVal == null || "null".equalsIgnoreCase(stringVal);
				}
				throw new WhereClauseException("operator 'is " + this.getStringValue() + "' not supported");
			} else if (this._operator.toLowerCase().equals("in")) {
				Set<String> set = (Set<String>) this._compiledValue;
				return set.contains(stringVal);
			} else if (this._operator.toLowerCase().equals("not in")) {
				if (doubleVal != null) {
					try {
						for (String obj : (Set<String>) this._compiledValue) {
							if (doubleVal.equals(Double.parseDouble(obj)))
								return false;
						}
						return true;
					} catch (Exception e) {
						throw new WhereClauseException("Failed to evaluate " + doubleVal + " not in ()", e);
					}
				}
				Set<String> set = (Set<String>) this._compiledValue;
				return !set.contains(stringVal);
			} else if (this._operator.toLowerCase().equals("like")) {
				Pattern pattern = (Pattern) this._compiledValue;
				return pattern.matcher(stringVal.toLowerCase()).matches();
			} else if (this._operator.toLowerCase().equals("not like")) {
				Pattern pattern = (Pattern) this._compiledValue;
				return !pattern.matcher(stringVal.toLowerCase()).matches();
			}
			// Skip this._operator
			return false;
		} catch (Exception e) {
			throw new WhereClauseException("Invalid value " + val + " for " + this, e);
		}
	}

	private Double getDoubleValue() throws WhereClauseException {
		if (!(_compiledValue instanceof Number))
			return Double.NaN;
		return ((Number) _compiledValue).doubleValue();
	}

	private String getStringValue() {
		return _compiledValue.toString();
	}

	public List<String> getFields() {
		List<String> fields = new ArrayList<String>();
		if (this._field != null)
			fields.add(this._field);
		return fields;
	}

	public String getField() {
		return this._field;
	}

	public Object getValue() {
		if (_compiledValue == null)
			compile();
		return this._compiledValue;
	}

	public String toString() {
		return this._field + " " + this._operator + " " + this._value;
	}

	public static class TrueWhereClauseBlock extends WhereClauseBlock {
		public TrueWhereClauseBlock() {
			super(null, null, null);
		}

		boolean accepts(Object val) throws WhereClauseException {
			return true;
		}

		public boolean accepts(Map<String, Object> attributes) throws WhereClauseException {
			return true;
		}

		public boolean accepts(Map<String, Object> attributes, Map<String, String> aliases)
				throws WhereClauseException {
			return true;
		}

		public WhereClauseBlock compile() {
			return this;
		}
	}

	private static Set<String> stringToList(String stringList, char sep) {
		Set<String> result = new HashSet<String>();
		boolean insideQuotes = false;
		String value = "";
		for (int i = 0; i < stringList.length(); i++) {
			char c = stringList.charAt(i);
			if (c == '\'')
				insideQuotes = !insideQuotes;
			if (!insideQuotes && c == sep) {
				addValueToList(result, value);
				value = "";
			} else {
				value += c;
			}
		}

		// add the last value
		if (value != null && value.length() > 0)
			addValueToList(result, value);

		return result;
	}

	private static void addValueToList(Set<String> list, String value) {
		value = value.trim();
		if (value.startsWith("'") && value.endsWith("'")) {
			String string = value.substring(1, value.length() - 1);
			if (string.contains("''"))
				string = string.replaceAll("''", "'");
			list.add(string);
		} else {
			list.add(value);
		}
	}

}
