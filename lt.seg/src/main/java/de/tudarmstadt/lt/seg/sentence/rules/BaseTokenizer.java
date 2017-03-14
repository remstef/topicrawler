/*
 *  Copyright (c) 2016
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package de.tudarmstadt.lt.seg.sentence.rules;

import de.tudarmstadt.lt.seg.token.ITokenizer;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.Charset;

/**
 * Created by Steffen Remus.
 */
public class BaseTokenizer {

    private final String _tokenizer_type;

    static {
        try{
            DEFAULT = new BaseTokenizer();
        }catch(Exception e){
            throw new IllegalStateException(e);
        }
    }

    public static final BaseTokenizer DEFAULT;

    private BaseTokenizer() throws Exception {
        this(Thread.currentThread().getContextClassLoader().getResource("rulesets/sentence/default/tokenizer.txt"), Charset.forName("UTF-8"));
    }

    public BaseTokenizer(URL tokenizer_file_location, Charset cs) throws Exception {
        this(new InputStreamReader(tokenizer_file_location.openStream(), cs));
    }

    public BaseTokenizer(InputStreamReader r) throws Exception {
        String tokenizer_class = null;
        final BufferedReader br = new BufferedReader(r);
        for(String line = null; (line = br.readLine()) != null;){
            if (line.trim().isEmpty() || line.startsWith("#"))
                continue;
            if(tokenizer_class != null)
                throw new IllegalStateException(String.format("Found multiple tokenizer classes: '%s' and '%s'. Please specify exactly one base tokenizer. Use '#' for comments.", tokenizer_class, line.trim()));
            tokenizer_class = line.trim();
        }
        _tokenizer_type = tokenizer_class;
    }

    public ITokenizer newTokenizer() {
        try {
            @SuppressWarnings("unchecked")
            Class<ITokenizer> clazz = (Class<ITokenizer>) Class.forName(_tokenizer_type);
            return clazz.newInstance();
        }catch(ClassNotFoundException | InstantiationException | IllegalAccessException e){
            throw new RuntimeException(e);
        }
    }

}
