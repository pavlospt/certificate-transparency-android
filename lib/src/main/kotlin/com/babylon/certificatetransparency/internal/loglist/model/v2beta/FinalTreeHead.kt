/*
 * Copyright 2019 Babylon Partners Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.babylon.certificatetransparency.internal.loglist.model.v2beta

import com.google.gson.annotations.SerializedName

internal data class FinalTreeHead(
    @SerializedName("tree_size") val treeSize: Int,
    @SerializedName("sha256_root_hash") val sha256RootHash: String
) {
    init {
        require(treeSize >= 0)
        @Suppress("MagicNumber")
        require(sha256RootHash.length == 44)
    }
}
