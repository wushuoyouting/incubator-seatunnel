/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.seatunnel.config.impl;

import org.apache.seatunnel.config.ConfigException.BugOrBroken;
import org.apache.seatunnel.config.ConfigException.NotResolved;
import org.apache.seatunnel.config.ConfigMergeable;
import org.apache.seatunnel.config.ConfigObject;
import org.apache.seatunnel.config.ConfigOrigin;
import org.apache.seatunnel.config.ConfigRenderOptions;
import org.apache.seatunnel.config.ConfigValue;
import org.apache.seatunnel.config.ConfigValueType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

abstract class AbstractConfigObject extends AbstractConfigValue implements ConfigObject, Container {
    private final SimpleConfig config;

    protected AbstractConfigObject(ConfigOrigin origin) {
        super(origin);
        this.config = new SimpleConfig(this);
    }

    @Override
    public SimpleConfig toConfig() {
        return config;
    }

    @Override
    public AbstractConfigObject toFallbackValue() {
        return this;
    }

    @Override
    public abstract AbstractConfigObject withOnlyKey(String key);

    @Override
    public abstract AbstractConfigObject withoutKey(String key);

    @Override
    public abstract AbstractConfigObject withValue(String key, ConfigValue value);

    protected abstract AbstractConfigObject withOnlyPathOrNull(Path path);

    abstract AbstractConfigObject withOnlyPath(Path path);

    abstract AbstractConfigObject withoutPath(Path path);

    abstract AbstractConfigObject withValue(Path path, ConfigValue value);

    /**
     * This looks up the key with no transformation or type conversion of any
     * kind, and returns null if the key is not present. The object must be
     * resolved along the nodes needed to get the key or
     * ConfigException.NotResolved will be thrown.
     *
     * @param key key
     * @return the unmodified raw value or null
     */
    protected final AbstractConfigValue peekAssumingResolved(String key, Path originalPath) {
        try {
            return attemptPeekWithPartialResolve(key);
        } catch (NotResolved e) {
            throw ConfigImpl.improveNotResolved(originalPath, e);
        }
    }

    /**
     * Look up the key on an only-partially-resolved object, with no
     * transformation or type conversion of any kind; if 'this' is not resolved
     * then try to look up the key anyway if possible.
     *
     * @param key key to look up
     * @return the value of the key, or null if known not to exist
     * @throws NotResolved if can't figure out key's value (or existence) without more
     *                                     resolving
     */
    abstract AbstractConfigValue attemptPeekWithPartialResolve(String key);

    /**
     * Looks up the path with no transformation or type conversion. Returns null
     * if the path is not found; throws ConfigException.NotResolved if we need
     * to go through an unresolved node to look up the path.
     */
    protected AbstractConfigValue peekPath(Path path) {
        return peekPath(this, path);
    }

    private static AbstractConfigValue peekPath(AbstractConfigObject self, Path path) {
        try {
            // we'll fail if anything along the path can't
            // be looked at without resolving.
            Path next = path.remainder();
            AbstractConfigValue v = self.attemptPeekWithPartialResolve(path.first());

            if (next == null) {
                return v;
            }
            if (v instanceof AbstractConfigObject) {
                return peekPath((AbstractConfigObject) v, next);
            }
            return null;
        } catch (NotResolved e) {
            throw ConfigImpl.improveNotResolved(path, e);
        }
    }

    @Override
    public ConfigValueType valueType() {
        return ConfigValueType.OBJECT;
    }

    protected abstract AbstractConfigObject newCopy(ResolveStatus status, ConfigOrigin origin);

    @Override
    protected AbstractConfigObject newCopy(ConfigOrigin origin) {
        return newCopy(resolveStatus(), origin);
    }

    @Override
    protected AbstractConfigObject constructDelayedMerge(ConfigOrigin origin,
                                                         List<AbstractConfigValue> stack) {
        return new ConfigDelayedMergeObject(origin, stack);
    }

    @Override
    protected abstract AbstractConfigObject mergedWithObject(AbstractConfigObject fallback);

    @Override
    public AbstractConfigObject withFallback(ConfigMergeable mergeable) {
        return (AbstractConfigObject) super.withFallback(mergeable);
    }

    static ConfigOrigin mergeOrigins(
            Collection<? extends AbstractConfigValue> stack) {
        if (stack.isEmpty()) {
            throw new BugOrBroken(
                    "can't merge origins on empty list");
        }
        List<ConfigOrigin> origins = new ArrayList<ConfigOrigin>();
        ConfigOrigin firstOrigin = null;
        int numMerged = 0;
        for (AbstractConfigValue v : stack) {
            if (firstOrigin == null) {
                firstOrigin = v.origin();
            }

            if (v instanceof AbstractConfigObject
                    && ((AbstractConfigObject) v).resolveStatus() == ResolveStatus.RESOLVED
                    && ((ConfigObject) v).isEmpty()) {
                // don't include empty files or the .empty()
                // config in the description, since they are
                // likely to be "implementation details"
            } else {
                origins.add(v.origin());
                numMerged += 1;
            }
        }

        if (numMerged == 0) {
            // the configs were all empty, so just use the first one
            origins.add(firstOrigin);
        }

        return SimpleConfigOrigin.mergeOrigins(origins);
    }

    static ConfigOrigin mergeOrigins(AbstractConfigObject... stack) {
        return mergeOrigins(Arrays.asList(stack));
    }

    @Override
    abstract ResolveResult<? extends AbstractConfigObject> resolveSubstitutions(ResolveContext context,
                                                                                ResolveSource source)
            throws NotPossibleToResolve;

    @Override
    abstract AbstractConfigObject relativized(final Path prefix);

    @Override
    public abstract AbstractConfigValue get(Object key);

    @Override
    protected abstract void render(StringBuilder sb, int indent, boolean atRoot, ConfigRenderOptions options);

    private static UnsupportedOperationException weAreImmutable(String method) {
        return new UnsupportedOperationException("ConfigObject is immutable, you can't call Map." + method);
    }

    @Override
    public void clear() {
        throw weAreImmutable("clear");
    }

    @Override
    public ConfigValue put(String arg0, ConfigValue arg1) {
        throw weAreImmutable("put");
    }

    @Override
    public void putAll(Map<? extends String, ? extends ConfigValue> arg0) {
        throw weAreImmutable("putAll");
    }

    @Override
    public ConfigValue remove(Object arg0) {
        throw weAreImmutable("remove");
    }

    @Override
    public AbstractConfigObject withOrigin(ConfigOrigin origin) {
        return (AbstractConfigObject) super.withOrigin(origin);
    }
}
