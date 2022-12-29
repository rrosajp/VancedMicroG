/*
 * SPDX-FileCopyrightText: 2016 microG Project Team
 * SPDX-License-Identifier: Apache-2.0 AND CC-BY-4.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.tasks;

import org.microg.gms.common.PublicApi;
import org.microg.gms.tasks.TaskImpl;

@PublicApi
public class TaskCompletionSource<TResult> {
    private final TaskImpl<TResult> task = new TaskImpl<>();

    /**
     * Creates an instance of {@link TaskCompletionSource}.
     */
    public TaskCompletionSource() {
    }

    /**
     * Returns the Task.
     */
    public Task<TResult> getTask() {
        return task;
    }

    /**
     * Completes the Task with the specified exception.
     *
     * @throws IllegalStateException if the Task is already complete
     */
    public void setException(Exception e) {
        task.setException(e);
    }

    /**
     * Completes the Task with the specified exception, unless the Task has already completed.
     * If the Task has already completed, the call does nothing.
     *
     */
    public void trySetException(Exception e) {
        try {
            setException(e);
        } catch (DuplicateTaskCompletionException ignored) {
        }
    }

    /**
     * Completes the Task with the specified result.
     *
     * @throws IllegalStateException if the Task is already complete
     */
    public void setResult(TResult result) {
        task.setResult(result);
    }

}
