/**
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

package org.waveprotocol.wave.model.document.util;

import org.waveprotocol.wave.model.util.Preconditions;

import jsinterop.annotations.JsIgnore;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

/**
 * An immutable range of two locations integers
 */
@JsType(namespace = "swellrt.Editor", name = "Range")
public final class Range {

  public static final Range ALL = new Range(0,0); 
  
  public static Range create(int start, int end) {
    return new Range(start, end);
  }

  
  private final int start;
  private final int end;

  /**
   * Construct a range
   *
   * @param start
   * @param end
   */
  @JsIgnore
  public Range(int start, int end) {
    if (start < 0 || start > end) {
      Preconditions.illegalArgument("Bad range: (" + start + ", " + end + ")");
    }
    this.start = start;
    this.end = end;
  }

  /**
   * Construct a collapsed range
   *
   * @param collapsedAt
   */
  @JsIgnore
  public Range(int collapsedAt) {
    this(collapsedAt, collapsedAt);
  }

  /**
   * @return start point
   */
  @JsProperty
  public int getStart() {
    return start;
  }

  /**
   * @return end point
   */
  @JsProperty
  public int getEnd() {
    return end;
  }

  /**
   * @return true if the range is collapsed
   */
  public boolean isCollapsed() {
    return start == end;
  }

  /**
   * {@inheritDoc}
   */
  @JsIgnore
  @Override
  public int hashCode() {
    return start + 37 * end;
  }

  /**
   * {@inheritDoc}
   */
  @JsIgnore
  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (!(obj instanceof Range)) return false;
    final Range other = (Range) obj;
    if (end != other.end) return false;
    if (start != other.start) return false;
    return true;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    return "Range(" + getStart()
        + (isCollapsed() ? "" : "-" + getEnd())
        + ")";
  }
  
  public boolean contains(Range r) {
    return this.start <= r.start && r.end <= this.end; 
  }

}
