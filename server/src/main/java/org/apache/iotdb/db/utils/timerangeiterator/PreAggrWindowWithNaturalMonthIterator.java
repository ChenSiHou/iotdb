/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.iotdb.db.utils.timerangeiterator;

import org.apache.iotdb.db.utils.datastructure.TimeSelector;
import org.apache.iotdb.tsfile.read.common.TimeRange;

public class PreAggrWindowWithNaturalMonthIterator implements ITimeRangeIterator {

  private static final int HEAP_MAX_SIZE = 100;

  private final boolean isAscending;
  private final TimeSelector timeBoundaryHeap;

  private final AggrWindowIterator aggrWindowIterator;
  private long curStartTimeForIterator;

  private long lastEndTime;

  public PreAggrWindowWithNaturalMonthIterator(
      long startTime,
      long endTime,
      long interval,
      long slidingStep,
      boolean isAscending,
      boolean isSlidingStepByMonth,
      boolean isIntervalByMonth) {
    this.isAscending = isAscending;
    this.timeBoundaryHeap = new TimeSelector(HEAP_MAX_SIZE, isAscending);
    this.aggrWindowIterator =
        new AggrWindowIterator(
            startTime,
            endTime,
            interval,
            slidingStep,
            isAscending,
            isSlidingStepByMonth,
            isIntervalByMonth);
    initHeap();
  }

  @Override
  public TimeRange getFirstTimeRange() {
    long retStartTime = timeBoundaryHeap.pollFirst();
    lastEndTime = timeBoundaryHeap.first();
    return new TimeRange(retStartTime, lastEndTime);
  }

  @Override
  public TimeRange getNextTimeRange(long curStartTime) {
    if (lastEndTime >= curStartTimeForIterator) {
      tryToExpandHeap();
    }
    if (timeBoundaryHeap.isEmpty()) {
      return null;
    }
    long retStartTime = timeBoundaryHeap.pollFirst();
    if (retStartTime >= curStartTimeForIterator) {
      tryToExpandHeap();
    }
    if (timeBoundaryHeap.isEmpty()) {
      return null;
    }
    lastEndTime = timeBoundaryHeap.first();
    return new TimeRange(retStartTime, lastEndTime);
  }

  private void initHeap() {
    TimeRange firstTimeRange = aggrWindowIterator.getFirstTimeRange();
    timeBoundaryHeap.add(firstTimeRange.getMin());
    timeBoundaryHeap.add(firstTimeRange.getMax());
    curStartTimeForIterator = firstTimeRange.getMin();

    tryToExpandHeap();
  }

  private void tryToExpandHeap() {
    TimeRange curTimeRange = aggrWindowIterator.getNextTimeRange(curStartTimeForIterator);
    while (curTimeRange != null && timeBoundaryHeap.size() < HEAP_MAX_SIZE) {
      timeBoundaryHeap.add(curTimeRange.getMin());
      timeBoundaryHeap.add(curTimeRange.getMax());
      curStartTimeForIterator = curTimeRange.getMin();

      curTimeRange = aggrWindowIterator.getNextTimeRange(curStartTimeForIterator);
    }
  }

  @Override
  public boolean isAscending() {
    return isAscending;
  }
}
