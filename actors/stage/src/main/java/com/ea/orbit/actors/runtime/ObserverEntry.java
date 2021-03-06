/*
 Copyright (C) 2015 Electronic Arts Inc.  All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions
 are met:

 1.  Redistributions of source code must retain the above copyright
     notice, this list of conditions and the following disclaimer.
 2.  Redistributions in binary form must reproduce the above copyright
     notice, this list of conditions and the following disclaimer in the
     documentation and/or other materials provided with the distribution.
 3.  Neither the name of Electronic Arts, Inc. ("EA") nor the names of
     its contributors may be used to endorse or promote products derived
     from this software without specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY ELECTRONIC ARTS AND ITS CONTRIBUTORS "AS IS" AND ANY
 EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 DISCLAIMED. IN NO EVENT SHALL ELECTRONIC ARTS OR ITS CONTRIBUTORS BE LIABLE FOR ANY
 DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.ea.orbit.actors.runtime;

import com.ea.orbit.actors.concurrent.MultiExecutionSerializer;
import com.ea.orbit.concurrent.Task;
import com.ea.orbit.concurrent.TaskFunction;

import java.lang.ref.WeakReference;

public class ObserverEntry<T> implements LocalObjects.LocalObjectEntry<T>
{
    private final RemoteReference<T> reference;
    private final WeakReference<T> object;
    private MultiExecutionSerializer<Object> executionSerializer;

    public ObserverEntry(final RemoteReference reference, final T object)
    {

        this.reference = reference;
        this.object = new WeakReference<>(object);
    }

    @Override
    public RemoteReference<T> getRemoteReference()
    {
        return reference;
    }

    @Override
    public T getObject()
    {
        return object.get();
    }

    @Override
    public <R> Task<R> run(final TaskFunction<LocalObjects.LocalObjectEntry<T>, R> function)
    {
        return function.apply(this);
    }

    public void setExecutionSerializer(final MultiExecutionSerializer<Object> executionSerializer)
    {
        this.executionSerializer = executionSerializer;
    }
}
