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

import java.io.UnsupportedEncodingException;
import java.util.Base64;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * 从固定token里解码后取出验证码
 *
 * @author weizhanfei
 * @see LongSum
 * @since 1.8.1
 */
public class EAgetCodeFromToken extends AbstractFunction {
    private static final List<String> desc = new LinkedList<>();

    private static final String KEY = "__EA_getCodeFromToken"; //$NON-NLS-1$

    static {
        desc.add("token:登录时从系统返回的token"); //$NON-NLS-1$
        desc.add("type:解码类型"); //$NON-NLS-1$
    }

    private Object[] values;

    /**
     * No-arg constructor.
     */
    public EAgetCodeFromToken() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String execute(SampleResult previousResult, Sampler currentSampler) throws InvalidVariableException {
        String s = "";
        if (values.length > 0) {
            try {
				s = base64Decode(((CompoundVariable) values[0]).execute().trim());
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
            return s.substring(37, 41);
        } else {
            return "返回错误 ";
        }

    }

    /***
     * base 64解码
     */

    public static String base64Decode(String codeString) throws UnsupportedEncodingException {
        byte[] asBytes = Base64.getDecoder().decode(codeString.substring(21, 101));
        return new String(asBytes, "utf-8");

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setParameters(Collection<CompoundVariable> parameters) throws InvalidVariableException {
        checkMinParameterCount(parameters, 2);
        values = parameters.toArray();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getReferenceKey() {
        return KEY;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> getArgumentDesc() {
        return desc;
    }

}

