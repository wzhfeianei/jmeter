/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.apache.jmeter.functions;

import org.apache.jmeter.engine.util.CompoundVariable;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.samplers.Sampler;

import java.security.MessageDigest;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * Provides an intSum function that adds two or more integer values.
 * 
 * @author weizhanfei
 * @see LongSum
 * @since 1.8.1
 */
public class EASign extends AbstractFunction {
	private static final List<String> desc = new LinkedList<>();

	private static final String KEY = "__EA_ZHENGCAI_Sign"; //$NON-NLS-1$

	static {
		desc.add("access_token:默认值access_token"); //$NON-NLS-1$
		desc.add("time：当前时间"); //$NON-NLS-1$
		desc.add("user：用户名"); //$NON-NLS-1$
		desc.add("password：密码（直接使用字符时括号要用反斜杠转义)"); //$NON-NLS-1$
	}

	private Object[] values;

	/**
	 * No-arg constructor.
	 */
	public EASign() {
	}

	/** {@inheritDoc} */
	@Override
	public String execute(SampleResult previousResult, Sampler currentSampler) throws InvalidVariableException {
		String s = "";

		for (int i = 0; i < values.length; i++) {
			s += ((CompoundVariable) values[i]).execute().trim().replace("\\", "");
			// System.out.println(s);
		}

		return string2MD5(s).toUpperCase();

	}

	/***
	 * MD5加码 生成32位md5码
	 */
	public static String string2MD5(String inStr) {
		MessageDigest md5 = null;
		try {
			md5 = MessageDigest.getInstance("MD5");
		} catch (Exception e) {
			System.out.println(e.toString());
			e.printStackTrace();
			return "";
		}
		char[] charArray = inStr.toCharArray();
		byte[] byteArray = new byte[charArray.length];

		for (int i = 0; i < charArray.length; i++)
			byteArray[i] = (byte) charArray[i];
		byte[] md5Bytes = md5.digest(byteArray);
		StringBuffer hexValue = new StringBuffer();
		for (int i = 0; i < md5Bytes.length; i++) {
			int val = ((int) md5Bytes[i]) & 0xff;
			if (val < 16)
				hexValue.append("0");
			hexValue.append(Integer.toHexString(val));
		}
		return hexValue.toString();

	}

	/** {@inheritDoc} */
	@Override
	public void setParameters(Collection<CompoundVariable> parameters) throws InvalidVariableException {
		checkMinParameterCount(parameters, 2);
		values = parameters.toArray();
	}

	/** {@inheritDoc} */
	@Override
	public String getReferenceKey() {
		return KEY;
	}

	/** {@inheritDoc} */
	@Override
	public List<String> getArgumentDesc() {
		return desc;
	}
}
