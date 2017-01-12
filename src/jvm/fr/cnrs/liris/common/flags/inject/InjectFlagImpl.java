// Large portions of code are copied from Google's Bazel.
/*
 * Copyright 2014 The Bazel Authors. All rights reserved.
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the
 * NOTICE file distributed with this work for additional information regarding copyright ownership.  The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License
 * for the specific language governing permissions and limitations under the License.
 */

package fr.cnrs.liris.common.flags.inject;


import java.io.Serializable;
import java.lang.annotation.Annotation;

/* Pattern copied from com.google.inject.name.NamedImpl */
public class InjectFlagImpl implements InjectFlag, Serializable {
    private final String value;

    public InjectFlagImpl(String value) {
        this.value = value;
    }

    public String value() {
        return this.value;
    }

    public int hashCode() {
        // This is specified in java.lang.Annotation.
        return (127 * "value".hashCode()) ^ value.hashCode();
    }

    public boolean equals(Object o) {
        if (!(o instanceof InjectFlag)) {
            return false;
        }
        InjectFlag other = (InjectFlag) o;
        return value.equals(other.value());
    }

    public String toString() {
        return "@" + InjectFlag.class.getName() + "(value=" + value + ")";
    }

    public Class<? extends Annotation> annotationType() {
        return InjectFlag.class;
    }

    private static final long serialVersionUID = 0;
}