/*
 * Copyright (C) 2014 Modified by Sedona Paris
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fr.sedona.volley.manager;

/**
 * Informations about the query result
 */
public class ResultInfo {

    public int httpCode;

    /**
     * Status query result.
     */
    public CODE_QUERY codeQuery;

    /**
     * Timestamp Unix (POSIX) in millisecond of the receiving of query result.
     */
    public long dataDatetime = 0;

    /**
     * Indicates if the current query data result will be updated in a next callback with the same query id.
     */
    public boolean dataIsRefreshing = false;

    private Object tag;

    /**
     * Data of the query result in failure.
     */
    public byte[] errorResponse;

    public Object getTag() {
        return tag;
    }

    public void setTag(Object tag) {
        this.tag = tag;
    }

    public static enum CODE_QUERY{
        SUCCESS, SERVER_ERROR, NETWORK_ERROR, NOT_AUTHORIZED, NOT_FOUND, ERROR_ALREADY_MANAGED
    }

    public ResultInfo() {
    }

    public ResultInfo(CODE_QUERY codeQuery) {
        this.codeQuery = codeQuery;
    }

    /**
     * @return True if the query is a success. False otherwise.
     */
    public boolean isSuccess() {
        return codeQuery == CODE_QUERY.SUCCESS;
    }
}
