/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.commons.log.logback.internal.joran;

import java.util.Objects;

import ch.qos.logback.core.model.Model;
import ch.qos.logback.core.model.processor.PhaseIndicator;
import ch.qos.logback.core.model.processor.ProcessingPhase;

/**
 * Model for the appender-ref-osgi element
 */
@PhaseIndicator(phase = ProcessingPhase.SECOND)
public class OsgiAppenderRefModel extends Model {

    private static final long serialVersionUID = 5238705468395447547L;

    /**
     * The referenced appender name
     */
    private String ref;

    /**
     * Factory method to create an instance of this model
     */
    @Override
    protected OsgiAppenderRefModel makeNewInstance() {
        return new OsgiAppenderRefModel();
    }

    /**
     * Mirror the state of the the supplied model
     *
     * @param that the model to mirror
     */
    @Override
    protected void mirror(Model that) {
        OsgiAppenderRefModel actual = (OsgiAppenderRefModel) that;
        super.mirror(actual);
        this.ref = actual.ref;
    }

    /**
     * Get the appender ref name
     *
     * @return the name of the appender to reference
     */
    public String getRef() {
        return ref;
    }

    /**
     * Set the appender ref name
     *
     * @param ref the name of the appender to reference
     */
    public void setRef(String ref) {
        this.ref = ref;
    }

    /**
     * Returns a hash code value for the object
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + Objects.hash(ref);
        return result;
    }

    /**
     * Returns wither the supplied object is equals to this one
     *
     * @param obj the other object to compare
     * @return true if the object is equal, false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        if (!super.equals(obj)) return false;
        OsgiAppenderRefModel other = (OsgiAppenderRefModel) obj;
        return Objects.equals(ref, other.ref);
    }
}
