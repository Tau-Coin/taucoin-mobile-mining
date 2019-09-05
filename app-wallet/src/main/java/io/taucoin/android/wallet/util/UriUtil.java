/**
 * Copyright 2018 Taucoin Core Developers.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.taucoin.android.wallet.util;

import android.net.Uri;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import io.taucoin.foundation.util.StringUtil;

public class UriUtil {

    public static String getBaseUrl(Uri uri) {
        if(uri != null){
            return uri.getScheme() +
                    "://" +
                    uri.getAuthority()
                    + File.separator;
        }
        return "";
    }

    public static String getPath(Uri uri) {
        return getPath(uri, true);
    }

    public static String getPath(Uri uri, boolean isNeedSeparator) {
        if(uri != null){
            String path = uri.getPath();
            if(StringUtil.isNotEmpty(path)){
                if(!isNeedSeparator && path.startsWith("/")){
                    path = path.substring(1);
                }
                return path;
            }
        }
        return "";
    }

    public static Map<String, String> getQueryMap(Uri uri) {
        Map<String, String> map = new HashMap<>();
        if(uri != null){
            Set<String> names = uri.getQueryParameterNames();
            for (String name : names) {
                String value = uri.getQueryParameter(name);
                if(StringUtil.isNotEmpty(value)){
                    map.put(name, value);
                }
            }
        }
        return map;
    }
}