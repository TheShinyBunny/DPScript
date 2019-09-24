package com.shinysponge.dpscript.old_tokenizer_which_is_bad;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TokenizerBad {

	private List<TokenData> tokenDatas;
	
	private String str;
	
	private TokenBad lastToken;
	private boolean pushBack;
	
	public TokenizerBad(String str) {
		this.tokenDatas = new ArrayList<>();
		this.str = str;
		
		tokenDatas.add(new TokenData(Pattern.compile("^([a-zA-Z][a-zA-Z0-9]*)"), TokenTypeBad.IDENTIFIER));
		tokenDatas.add(new TokenData(Pattern.compile("^((-)?[0-9]+)"), TokenTypeBad.INTEGER_LITERAL));
		tokenDatas.add(new TokenData(Pattern.compile("^(\".*\")"), TokenTypeBad.STRING_LITERAL));
		tokenDatas.add(new TokenData(Pattern.compile("^(\n)"), TokenTypeBad.LINE_END));
		
		for (String t : new String[] { "\\=", "\\(", "\\)", "\\.", "\\,", "\\{", "\\}"}) {
			tokenDatas.add(new TokenData(Pattern.compile("^(" + t + ")"), TokenTypeBad.TOKEN));
		}

		for (String t : new String[] { "\\+", "\\-", "\\*", "\\/", "\\%" }) {
			tokenDatas.add(new TokenData(Pattern.compile("^(" + t + ")"), TokenTypeBad.OPERATOR));
		}
	}
	
	public TokenBad nextToken() {
		str = str.trim();

		if (pushBack) {
			pushBack = false;
			return lastToken;
		}
		
		if (str.isEmpty()) {
			return (lastToken = new TokenBad("", TokenTypeBad.EMPTY));
		}
		
		for (TokenData data : tokenDatas) {
			Matcher matcher = data.getPattern().matcher(str);
			
			if (matcher.find()) {
				String token = matcher.group().trim();
				str = matcher.replaceFirst("");
				
				if (data.getType() == TokenTypeBad.STRING_LITERAL) {
					return (lastToken = new TokenBad(token.substring(1, token.length() - 1), TokenTypeBad.STRING_LITERAL));
				}
				
				else {
					return (lastToken = new TokenBad(token, data.getType()));
				}
			}
		}
		
		throw new IllegalStateException("Could not parse " + str);
	}
	
	public boolean hasNextToken() {
		return !str.isEmpty();
	}
	
	public void pushBack() {
		if (lastToken != null) {
			this.pushBack = true;
		}
	}
}