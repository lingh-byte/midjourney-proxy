package com.github.novicezk.midjourney.util;

import cn.hutool.core.text.CharSequenceUtil;
import com.github.novicezk.midjourney.enums.TaskAction;
import eu.maxschuster.dataurl.DataUrl;
import eu.maxschuster.dataurl.DataUrlSerializer;
import eu.maxschuster.dataurl.IDataUrlSerializer;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@UtilityClass
public class ConvertUtils {
	/**
	 * MJ 完成消息可能在 {@literal <@bot>} 与 (mode) 之间插入 markdown 链接，如 [(Open on website for full quality)](url)，
	 * OPTIONAL_LINK 匹配这段可选内容。
	 */
	private static final String OPTIONAL_LINK = "\\s*(?:\\[.*?\\]\\(.*?\\)\\s*)?";
	/** bot mention + optional link + (mode) */
	private static final String BOT_MENTION_MODE = "<@\\d+>" + OPTIONAL_LINK + "\\((.*?)\\)";

	public static final String CONTENT_REGEX = ".*?\\*\\*(.*)\\*\\*.+" + BOT_MENTION_MODE;
	public static final String IMAGINE_CONTENT_REGEX = "\\*\\*(.*)\\*\\* - " + BOT_MENTION_MODE;
	public static final String VARIATION_CONTENT_REGEX_1 = "\\*\\*(.*)\\*\\* - Variations by " + BOT_MENTION_MODE;
	public static final String VARIATION_CONTENT_REGEX_2 = "\\*\\*(.*)\\*\\* - Variations \\(.*?\\) by " + BOT_MENTION_MODE;
	public static final String UPSCALE_CONTENT_REGEX_1 = "\\*\\*(.*)\\*\\* - Upscaled \\(.*?\\) by " + BOT_MENTION_MODE;
	public static final String UPSCALE_CONTENT_REGEX_2 = "\\*\\*(.*)\\*\\* - Upscaled by " + BOT_MENTION_MODE;

	public static ContentParseData parseContent(String content) {
		return parseContent(content, CONTENT_REGEX);
	}

	public static ContentParseData parseContent(String content, String regex) {
		if (CharSequenceUtil.isBlank(content)) {
			return null;
		}
		Matcher matcher = Pattern.compile(regex).matcher(content);
		if (!matcher.find()) {
			if (content.contains("**") && content.contains("<@")) {
				log.warn("Content looks like a MJ message but regex did not match. content: {}, regex: {}", content, regex);
			}
			return null;
		}
		ContentParseData parseData = new ContentParseData();
		parseData.setPrompt(matcher.group(1));
		parseData.setStatus(matcher.group(2));
		return parseData;
	}

	public static List<DataUrl> convertBase64Array(List<String> base64Array) throws MalformedURLException {
		if (base64Array == null || base64Array.isEmpty()) {
			return Collections.emptyList();
		}
		IDataUrlSerializer serializer = new DataUrlSerializer();
		List<DataUrl> dataUrlList = new ArrayList<>();
		for (String base64 : base64Array) {
			DataUrl dataUrl = serializer.unserialize(base64);
			dataUrlList.add(dataUrl);
		}
		return dataUrlList;
	}

	public static String getPrimaryPrompt(String prompt) {
		Matcher matcher = Pattern.compile("\\x20+--[a-z]+.*$", Pattern.CASE_INSENSITIVE).matcher(prompt);
		prompt = matcher.replaceAll("");
		String regex = "https?://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]";
		matcher = Pattern.compile(regex).matcher(prompt);
		return matcher.replaceAll("<link>").replace("<<link>>", "<link>");
	}

	public static TaskChangeParams convertChangeParams(String content) {
		List<String> split = CharSequenceUtil.split(content, " ");
		if (split.size() != 2) {
			return null;
		}
		String action = split.get(1).toLowerCase();
		TaskChangeParams changeParams = new TaskChangeParams();
		changeParams.setId(split.get(0));
		if (action.charAt(0) == 'u') {
			changeParams.setAction(TaskAction.UPSCALE);
		} else if (action.charAt(0) == 'v') {
			changeParams.setAction(TaskAction.VARIATION);
		} else if (action.equals("r")) {
			changeParams.setAction(TaskAction.REROLL);
			return changeParams;
		} else {
			return null;
		}
		try {
			int index = Integer.parseInt(action.substring(1, 2));
			if (index < 1 || index > 4) {
				return null;
			}
			changeParams.setIndex(index);
		} catch (Exception e) {
			return null;
		}
		return changeParams;
	}

}
