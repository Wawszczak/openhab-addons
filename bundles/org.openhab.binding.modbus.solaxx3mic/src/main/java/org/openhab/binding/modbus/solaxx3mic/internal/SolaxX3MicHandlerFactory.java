/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.modbus.solaxx3mic.internal;

import static org.openhab.binding.modbus.solaxx3mic.internal.SolaxX3MicBindingConstants.*;

import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandlerFactory;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandlerFactory;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link SolaxX3MicHandlerFactory} is responsible for creating things and thing
 * handlers.
 *
 * @author Stanislaw Wawszczak - Initial contribution
 */
@NonNullByDefault
@Component(configurationPid = "binding.solaxx3mic", service = ThingHandlerFactory.class)
public class SolaxX3MicHandlerFactory extends BaseThingHandlerFactory {

    /**
     * Logger instance
     */
    private final Logger logger = LoggerFactory.getLogger(SolaxX3MicHandlerFactory.class);

    private static final Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = Set.of(THING_TYPE_INVERTER);

    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        return SUPPORTED_THING_TYPES_UIDS.contains(thingTypeUID);
    }

    @Override
    protected @Nullable ThingHandler createHandler(Thing thing) {
        ThingTypeUID thingTypeUID = thing.getThingTypeUID();

        if (THING_TYPE_INVERTER.equals(thingTypeUID)) {
            logger.debug("New InverterHandler created");
            return new SolaxX3MicHandler(thing);
        }

        return null;
    }
}
