/*
 * SoulFire
 * Copyright (C) 2024  AlexProgrammerDE
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package net.pistonmaster.soulfire.server.viaversion;

import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import net.raphimc.viaaprilfools.api.AprilFoolsProtocolVersion;
import net.raphimc.viabedrock.api.BedrockProtocolVersion;
import net.raphimc.vialegacy.api.LegacyProtocolVersion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class SWVersionConstants {
    public static final ProtocolVersion CURRENT_PROTOCOL_VERSION = ProtocolVersion.v1_20_3;

    static {
        // Initialize all classes
        doNothing(
                ProtocolVersion.getProtocols(),
                LegacyProtocolVersion.PROTOCOLS,
                BedrockProtocolVersion.PROTOCOLS,
                AprilFoolsProtocolVersion.PROTOCOLS
        );
    }

    private SWVersionConstants() {
    }

    @SuppressWarnings("unused")
    private static void doNothing(Object... objects) {
        // Do nothing
    }

    public static List<ProtocolVersion> getVersionsSorted() {
        var normalVersions = new ArrayList<ProtocolVersion>();
        var legacyVersions = new ArrayList<ProtocolVersion>();
        for (var version : ProtocolVersion.getProtocols()) {
            if (version == ProtocolVersion.unknown) {
                continue; // Exclude unknown versions
            }

            var versionId = version.getVersion();
            if (versionId > CURRENT_PROTOCOL_VERSION.getVersion()) {
                continue; // Exclude in-development versions
            }

            if (isLegacy(version)) {
                legacyVersions.add(version);
            } else {
                if (versionId <= ProtocolVersion.v_1_6_4.getVersion()
                        && versionId >= ProtocolVersion.v1_4_6.getVersion()) {
                    continue; // Remove built-in legacy versions, so we use ViaLegacy
                }

                normalVersions.add(version);
            }
        }

        normalVersions.sort(Comparator.comparingInt(ProtocolVersion::getVersion));

        legacyVersions.sort((o1, o2) -> {
            var index1 = LegacyProtocolVersion.PROTOCOLS.indexOf(o1);
            var index2 = LegacyProtocolVersion.PROTOCOLS.indexOf(o2);

            return Integer.compare(index1, index2);
        });

        // Sort special case
        var index = legacyVersions.indexOf(LegacyProtocolVersion.c0_28toc0_30);
        legacyVersions.remove(LegacyProtocolVersion.c0_30cpe);
        legacyVersions.add(index + 1, LegacyProtocolVersion.c0_30cpe);

        var merged = mergeLists(BedrockProtocolVersion.PROTOCOLS, legacyVersions, normalVersions);
        Collections.reverse(merged);

        return merged;
    }

    @SafeVarargs
    private static <T> List<T> mergeLists(List<T>... versions) {
        var result = new ArrayList<T>();

        for (var version : versions) {
            result.addAll(version);
        }

        return result;
    }

    public static boolean isLegacy(ProtocolVersion version) {
        return LegacyProtocolVersion.PROTOCOLS.contains(version);
    }

    public static boolean isBedrock(ProtocolVersion version) {
        return BedrockProtocolVersion.PROTOCOLS.contains(version);
    }

    public static boolean isAprilFools(ProtocolVersion version) {
        return AprilFoolsProtocolVersion.PROTOCOLS.contains(version);
    }
}
