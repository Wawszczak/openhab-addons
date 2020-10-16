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
package org.openhab.binding.systeminfo.internal.model;

import java.math.BigDecimal;

import org.apache.commons.lang.ArrayUtils;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.StringType;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.ComputerSystem;
import oshi.hardware.Display;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HWDiskStore;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.NetworkIF;
import oshi.hardware.PowerSource;
import oshi.hardware.Sensors;
import oshi.software.os.OSFileStore;
import oshi.software.os.OSProcess;
import oshi.software.os.OperatingSystem;
import oshi.util.EdidUtil;

/**
 * This implementation of {@link SysteminfoInterface} is using the open source library OSHI to provide system
 * information. OSHI is a free JNA-based (native) Operating System and Hardware Information library for Java.
 *
 * @author Svilen Valkanov - Initial contribution
 * @author Lyubomir Papazov - Move the initialization logic that could potentially take long time to the
 *         initializeSysteminfo method
 * @author Christoph Weitkamp - Update to OSHI 3.13.0 - Replaced deprecated method
 *         CentralProcessor#getSystemSerialNumber()
 * @author Wouter Born - Update to OSHI 4.0.0 and add null annotations
 *
 * @see <a href="https://github.com/oshi/oshi">OSHI GitHub repository</a>
 */
@NonNullByDefault
@Component(service = SysteminfoInterface.class)
public class OSHISysteminfo implements SysteminfoInterface {

    private final Logger logger = LoggerFactory.getLogger(OSHISysteminfo.class);

    private @NonNullByDefault({}) HardwareAbstractionLayer hal;

    // Dynamic objects (may be queried repeatedly)
    private @NonNullByDefault({}) GlobalMemory memory;
    private @NonNullByDefault({}) CentralProcessor cpu;
    private @NonNullByDefault({}) Sensors sensors;

    // Static objects, should be recreated on each request
    private @NonNullByDefault({}) ComputerSystem computerSystem;
    private @NonNullByDefault({}) OperatingSystem operatingSystem;
    private @NonNullByDefault({}) NetworkIF[] networks;
    private @NonNullByDefault({}) Display[] displays;
    private @NonNullByDefault({}) OSFileStore[] fileStores;
    private @NonNullByDefault({}) PowerSource[] powerSources;
    private @NonNullByDefault({}) HWDiskStore[] drives;

    public static final int PRECISION_AFTER_DECIMAL_SIGN = 1;

    /**
     * Some of the methods used in this constructor execute native code and require execute permissions
     *
     */
    public OSHISysteminfo() {
        logger.debug("OSHISysteminfo service is created");
    }

    @Override
    public void initializeSysteminfo() {
        logger.debug("OSHISysteminfo service starts initializing");

        SystemInfo systemInfo = new SystemInfo();
        hal = systemInfo.getHardware();

        // Doesn't need regular update, they may be queried repeatedly
        memory = hal.getMemory();
        cpu = hal.getProcessor();
        sensors = hal.getSensors();

        computerSystem = hal.getComputerSystem();
        operatingSystem = systemInfo.getOperatingSystem();
        networks = hal.getNetworkIFs();
        displays = hal.getDisplays();
        fileStores = operatingSystem.getFileSystem().getFileStores();
        powerSources = hal.getPowerSources();
        drives = hal.getDiskStores();
    }

    private Object getDevice(Object @Nullable [] devices, int index) throws DeviceNotFoundException {
        if ((devices == null) || (devices.length <= index)) {
            throw new DeviceNotFoundException("Device with index: " + index + " can not be found!");
        }
        return devices[index];
    }

    private OSProcess getProcess(int pid) throws DeviceNotFoundException {
        OSProcess process = operatingSystem.getProcess(pid);
        if (process == null) {
            throw new DeviceNotFoundException("Error while getting information for process with PID " + pid);
        }
        return process;
    }

    @Override
    public StringType getOsFamily() {
        String osFamily = operatingSystem.getFamily();
        return new StringType(osFamily);
    }

    @Override
    public StringType getOsManufacturer() {
        String osManufacturer = operatingSystem.getManufacturer();
        return new StringType(osManufacturer);
    }

    @Override
    public StringType getOsVersion() {
        String osVersion = operatingSystem.getVersionInfo().toString();
        return new StringType(osVersion);
    }

    @Override
    public StringType getCpuName() {
        String name = cpu.getProcessorIdentifier().getName();
        return new StringType(name);
    }

    @Override
    public StringType getCpuDescription() {
        String model = cpu.getProcessorIdentifier().getModel();
        String family = cpu.getProcessorIdentifier().getFamily();
        String serialNumber = computerSystem.getSerialNumber();
        String identifier = cpu.getProcessorIdentifier().getIdentifier();
        String vendor = cpu.getProcessorIdentifier().getVendor();
        String architecture = cpu.getProcessorIdentifier().isCpu64bit() ? "64 bit" : "32 bit";
        String descriptionFormatString = "Model: %s %s,family: %s, vendor: %s, sn: %s, identifier: %s ";
        String description = String.format(descriptionFormatString, model, architecture, family, vendor, serialNumber,
                identifier);

        return new StringType(description);
    }

    @Override
    public DecimalType getCpuLogicalCores() {
        int logicalProcessorCount = cpu.getLogicalProcessorCount();
        return new DecimalType(logicalProcessorCount);
    }

    @Override
    public DecimalType getCpuPhysicalCores() {
        int physicalProcessorCount = cpu.getPhysicalProcessorCount();
        return new DecimalType(physicalProcessorCount);
    }

    @Override
    public DecimalType getMemoryTotal() {
        long totalMemory = memory.getTotal();
        totalMemory = getSizeInMB(totalMemory);
        return new DecimalType(totalMemory);
    }

    @Override
    public DecimalType getMemoryAvailable() {
        long availableMemory = memory.getAvailable();
        availableMemory = getSizeInMB(availableMemory);
        return new DecimalType(availableMemory);
    }

    @Override
    public DecimalType getMemoryUsed() {
        long totalMemory = memory.getTotal();
        long availableMemory = memory.getAvailable();
        long usedMemory = totalMemory - availableMemory;
        usedMemory = getSizeInMB(usedMemory);
        return new DecimalType(usedMemory);
    }

    @Override
    public DecimalType getStorageTotal(int index) throws DeviceNotFoundException {
        OSFileStore fileStore = (OSFileStore) getDevice(fileStores, index);
        fileStore.updateAtrributes();
        long totalSpace = fileStore.getTotalSpace();
        totalSpace = getSizeInMB(totalSpace);
        return new DecimalType(totalSpace);
    }

    @Override
    public DecimalType getStorageAvailable(int index) throws DeviceNotFoundException {
        OSFileStore fileStore = (OSFileStore) getDevice(fileStores, index);
        fileStore.updateAtrributes();
        long freeSpace = fileStore.getUsableSpace();
        freeSpace = getSizeInMB(freeSpace);
        return new DecimalType(freeSpace);
    }

    @Override
    public DecimalType getStorageUsed(int index) throws DeviceNotFoundException {
        OSFileStore fileStore = (OSFileStore) getDevice(fileStores, index);
        fileStore.updateAtrributes();
        long totalSpace = fileStore.getTotalSpace();
        long freeSpace = fileStore.getUsableSpace();
        long usedSpace = totalSpace - freeSpace;
        usedSpace = getSizeInMB(usedSpace);
        return new DecimalType(usedSpace);
    }

    @Override
    public @Nullable DecimalType getStorageAvailablePercent(int deviceIndex) throws DeviceNotFoundException {
        OSFileStore fileStore = (OSFileStore) getDevice(fileStores, deviceIndex);
        fileStore.updateAtrributes();
        long totalSpace = fileStore.getTotalSpace();
        long freeSpace = fileStore.getUsableSpace();
        if (totalSpace > 0) {
            double freePercentDecimal = (double) freeSpace / (double) totalSpace;
            BigDecimal freePercent = getPercentsValue(freePercentDecimal);
            return new DecimalType(freePercent);
        } else {
            return null;
        }
    }

    @Override
    public @Nullable DecimalType getStorageUsedPercent(int deviceIndex) throws DeviceNotFoundException {
        OSFileStore fileStore = (OSFileStore) getDevice(fileStores, deviceIndex);
        fileStore.updateAtrributes();
        long totalSpace = fileStore.getTotalSpace();
        long freeSpace = fileStore.getUsableSpace();
        long usedSpace = totalSpace - freeSpace;
        if (totalSpace > 0) {
            double usedPercentDecimal = (double) usedSpace / (double) totalSpace;
            BigDecimal usedPercent = getPercentsValue(usedPercentDecimal);
            return new DecimalType(usedPercent);
        } else {
            return null;
        }
    }

    @Override
    public StringType getStorageName(int index) throws DeviceNotFoundException {
        OSFileStore fileStore = (OSFileStore) getDevice(fileStores, index);
        String name = fileStore.getName();
        return new StringType(name);
    }

    @Override
    public StringType getStorageType(int deviceIndex) throws DeviceNotFoundException {
        OSFileStore fileStore = (OSFileStore) getDevice(fileStores, deviceIndex);
        String type = fileStore.getType();
        return new StringType(type);
    }

    @Override
    public StringType getStorageDescription(int index) throws DeviceNotFoundException {
        OSFileStore fileStore = (OSFileStore) getDevice(fileStores, index);
        String description = fileStore.getDescription();
        return new StringType(description);
    }

    @Override
    public StringType getNetworkIp(int index) throws DeviceNotFoundException {
        NetworkIF netInterface = (NetworkIF) getDevice(networks, index);
        netInterface.updateAttributes();
        String[] ipAddresses = netInterface.getIPv4addr();
        String ipv4 = (String) getDevice(ipAddresses, 0);
        return new StringType(ipv4);
    }

    @Override
    public StringType getNetworkName(int index) throws DeviceNotFoundException {
        NetworkIF netInterface = (NetworkIF) getDevice(networks, index);
        String name = netInterface.getName();
        return new StringType(name);
    }

    @Override
    public StringType getNetworkDisplayName(int index) throws DeviceNotFoundException {
        NetworkIF netInterface = (NetworkIF) getDevice(networks, index);
        String adapterName = netInterface.getDisplayName();
        return new StringType(adapterName);
    }

    @Override
    public StringType getDisplayInformation(int index) throws DeviceNotFoundException {
        Display display = (Display) getDevice(displays, index);

        byte[] edid = display.getEdid();
        String manufacturer = EdidUtil.getManufacturerID(edid);
        String product = EdidUtil.getProductID(edid);
        String serialNumber = EdidUtil.getSerialNo(edid);
        int width = EdidUtil.getHcm(edid);
        int height = EdidUtil.getVcm(edid);

        String edidFormatString = "Product %s, manufacturer %s, SN: %s, Width: %d, Height: %d";
        String edidInfo = String.format(edidFormatString, product, manufacturer, serialNumber, width, height);
        return new StringType(edidInfo);
    }

    @Override
    public @Nullable DecimalType getSensorsCpuTemperature() {
        BigDecimal cpuTemp = new BigDecimal(sensors.getCpuTemperature());
        cpuTemp = cpuTemp.setScale(PRECISION_AFTER_DECIMAL_SIGN, BigDecimal.ROUND_HALF_UP);
        return cpuTemp.signum() == 1 ? new DecimalType(cpuTemp) : null;
    }

    @Override
    public @Nullable DecimalType getSensorsCpuVoltage() {
        BigDecimal cpuVoltage = new BigDecimal(sensors.getCpuVoltage());
        cpuVoltage = cpuVoltage.setScale(PRECISION_AFTER_DECIMAL_SIGN, BigDecimal.ROUND_HALF_UP);
        return cpuVoltage.signum() == 1 ? new DecimalType(cpuVoltage) : null;
    }

    @Override
    public @Nullable DecimalType getSensorsFanSpeed(int index) throws DeviceNotFoundException {
        int[] fanSpeeds = sensors.getFanSpeeds();
        int speed = (int) getDevice(ArrayUtils.toObject(fanSpeeds), index);
        return speed > 0 ? new DecimalType(speed) : null;
    }

    @Override
    public @Nullable DecimalType getBatteryRemainingTime(int index) throws DeviceNotFoundException {
        PowerSource powerSource = (PowerSource) getDevice(powerSources, index);
        powerSource.updateAttributes();
        double remainingTimeInSeconds = powerSource.getTimeRemainingEstimated();
        // The getTimeRemaining() method returns (-1.0) if is calculating or (-2.0) if the time is unlimited.
        BigDecimal remainingTime = getTimeInMinutes(remainingTimeInSeconds);
        return remainingTime.signum() == 1 ? new DecimalType(remainingTime) : null;
    }

    @Override
    public DecimalType getBatteryRemainingCapacity(int index) throws DeviceNotFoundException {
        PowerSource powerSource = (PowerSource) getDevice(powerSources, index);
        powerSource.updateAttributes();
        double remainingCapacity = powerSource.getRemainingCapacityPercent();
        BigDecimal remainingCapacityPercents = getPercentsValue(remainingCapacity);
        return new DecimalType(remainingCapacityPercents);
    }

    @Override
    public StringType getBatteryName(int index) throws DeviceNotFoundException {
        PowerSource powerSource = (PowerSource) getDevice(powerSources, index);
        String name = powerSource.getName();
        return new StringType(name);
    }

    @Override
    public @Nullable DecimalType getMemoryAvailablePercent() {
        long availableMemory = memory.getAvailable();
        long totalMemory = memory.getTotal();
        if (totalMemory > 0) {
            double freePercentDecimal = (double) availableMemory / (double) totalMemory;
            BigDecimal freePercent = getPercentsValue(freePercentDecimal);
            return new DecimalType(freePercent);
        } else {
            return null;
        }
    }

    @Override
    public @Nullable DecimalType getMemoryUsedPercent() {
        long availableMemory = memory.getAvailable();
        long totalMemory = memory.getTotal();
        long usedMemory = totalMemory - availableMemory;
        if (totalMemory > 0) {
            double usedPercentDecimal = (double) usedMemory / (double) totalMemory;
            BigDecimal usedPercent = getPercentsValue(usedPercentDecimal);
            return new DecimalType(usedPercent);
        } else {
            return null;
        }
    }

    @Override
    public StringType getDriveName(int deviceIndex) throws DeviceNotFoundException {
        HWDiskStore drive = (HWDiskStore) getDevice(drives, deviceIndex);
        String name = drive.getName();
        return new StringType(name);
    }

    @Override
    public StringType getDriveModel(int deviceIndex) throws DeviceNotFoundException {
        HWDiskStore drive = (HWDiskStore) getDevice(drives, deviceIndex);
        String model = drive.getModel();
        return new StringType(model);
    }

    @Override
    public StringType getDriveSerialNumber(int deviceIndex) throws DeviceNotFoundException {
        HWDiskStore drive = (HWDiskStore) getDevice(drives, deviceIndex);
        String serialNumber = drive.getSerial();
        return new StringType(serialNumber);
    }

    @Override
    public @Nullable DecimalType getSwapTotal() {
        long swapTotal = memory.getVirtualMemory().getSwapTotal();
        swapTotal = getSizeInMB(swapTotal);
        return new DecimalType(swapTotal);
    }

    @Override
    public @Nullable DecimalType getSwapAvailable() {
        long swapTotal = memory.getVirtualMemory().getSwapTotal();
        long swapUsed = memory.getVirtualMemory().getSwapUsed();
        long swapAvailable = swapTotal - swapUsed;
        swapAvailable = getSizeInMB(swapAvailable);
        return new DecimalType(swapAvailable);
    }

    @Override
    public @Nullable DecimalType getSwapUsed() {
        long swapUsed = memory.getVirtualMemory().getSwapUsed();
        swapUsed = getSizeInMB(swapUsed);
        return new DecimalType(swapUsed);
    }

    @Override
    public @Nullable DecimalType getSwapAvailablePercent() {
        long swapTotal = memory.getVirtualMemory().getSwapTotal();
        long swapUsed = memory.getVirtualMemory().getSwapUsed();
        long swapAvailable = swapTotal - swapUsed;
        if (swapTotal > 0) {
            double swapAvailablePercentDecimal = (double) swapAvailable / (double) swapTotal;
            BigDecimal swapAvailablePercent = getPercentsValue(swapAvailablePercentDecimal);
            return new DecimalType(swapAvailablePercent);
        } else {
            return null;
        }
    }

    @Override
    public @Nullable DecimalType getSwapUsedPercent() {
        long swapTotal = memory.getVirtualMemory().getSwapTotal();
        long swapUsed = memory.getVirtualMemory().getSwapUsed();
        if (swapTotal > 0) {
            double swapUsedPercentDecimal = (double) swapUsed / (double) swapTotal;
            BigDecimal swapUsedPercent = getPercentsValue(swapUsedPercentDecimal);
            return new DecimalType(swapUsedPercent);
        } else {
            return null;
        }
    }

    private long getSizeInMB(long sizeInBytes) {
        return Math.round(sizeInBytes / (1024D * 1024));
    }

    private BigDecimal getPercentsValue(double decimalFraction) {
        BigDecimal result = new BigDecimal(decimalFraction * 100);
        result = result.setScale(PRECISION_AFTER_DECIMAL_SIGN, BigDecimal.ROUND_HALF_UP);
        return result;
    }

    private BigDecimal getTimeInMinutes(double timeInSeconds) {
        BigDecimal timeInMinutes = new BigDecimal(timeInSeconds / 60);
        timeInMinutes = timeInMinutes.setScale(PRECISION_AFTER_DECIMAL_SIGN, BigDecimal.ROUND_UP);
        return timeInMinutes;
    }

    /**
     * {@inheritDoc}
     *
     * This information is available only on Mac and Linux OS.
     */
    @Override
    public @Nullable DecimalType getCpuLoad1() {
        BigDecimal avarageCpuLoad = getAvarageCpuLoad(1);
        return avarageCpuLoad.signum() == -1 ? null : new DecimalType(avarageCpuLoad);
    }

    /**
     * {@inheritDoc}
     *
     * This information is available only on Mac and Linux OS.
     */
    @Override
    public @Nullable DecimalType getCpuLoad5() {
        BigDecimal avarageCpuLoad = getAvarageCpuLoad(5);
        return avarageCpuLoad.signum() == -1 ? null : new DecimalType(avarageCpuLoad);
    }

    /**
     * {@inheritDoc}
     *
     * This information is available only on Mac and Linux OS.
     */
    @Override
    public @Nullable DecimalType getCpuLoad15() {
        BigDecimal avarageCpuLoad = getAvarageCpuLoad(15);
        return avarageCpuLoad.signum() == -1 ? null : new DecimalType(avarageCpuLoad);
    }

    private BigDecimal getAvarageCpuLoad(int timeInMunutes) {
        // This parameter is specified in OSHI Javadoc
        int index;
        switch (timeInMunutes) {
            case 1:
                index = 0;
                break;
            case 5:
                index = 1;
                break;
            case 15:
                index = 2;
                break;
            default:
                index = 2;
        }
        double processorLoads[] = cpu.getSystemLoadAverage(index + 1);
        BigDecimal result = new BigDecimal(processorLoads[index]);
        result = result.setScale(PRECISION_AFTER_DECIMAL_SIGN, BigDecimal.ROUND_HALF_UP);
        return result;
    }

    @Override
    public DecimalType getCpuUptime() {
        long seconds = operatingSystem.getSystemUptime();
        return new DecimalType(getTimeInMinutes(seconds));
    }

    @Override
    public DecimalType getCpuThreads() {
        int threadCount = operatingSystem.getThreadCount();
        return new DecimalType(threadCount);
    }

    @Override
    public StringType getNetworkMac(int networkIndex) throws DeviceNotFoundException {
        NetworkIF network = (NetworkIF) getDevice(networks, networkIndex);
        String mac = network.getMacaddr();
        return new StringType(mac);
    }

    @Override
    public DecimalType getNetworkPacketsReceived(int networkIndex) throws DeviceNotFoundException {
        NetworkIF network = (NetworkIF) getDevice(networks, networkIndex);
        network.updateAttributes();
        long packRecv = network.getPacketsRecv();
        return new DecimalType(packRecv);
    }

    @Override
    public DecimalType getNetworkPacketsSent(int networkIndex) throws DeviceNotFoundException {
        NetworkIF network = (NetworkIF) getDevice(networks, networkIndex);
        network.updateAttributes();
        long packSent = network.getPacketsSent();
        return new DecimalType(packSent);
    }

    @Override
    public DecimalType getNetworkDataSent(int networkIndex) throws DeviceNotFoundException {
        NetworkIF network = (NetworkIF) getDevice(networks, networkIndex);
        network.updateAttributes();
        long bytesSent = network.getBytesSent();
        return new DecimalType(getSizeInMB(bytesSent));
    }

    @Override
    public DecimalType getNetworkDataReceived(int networkIndex) throws DeviceNotFoundException {
        NetworkIF network = (NetworkIF) getDevice(networks, networkIndex);
        network.updateAttributes();
        long bytesRecv = network.getBytesRecv();
        return new DecimalType(getSizeInMB(bytesRecv));
    }

    @Override
    public @Nullable StringType getProcessName(int pid) throws DeviceNotFoundException {
        if (pid > 0) {
            OSProcess process = getProcess(pid);
            String name = process.getName();
            return new StringType(name);
        } else {
            return null;
        }
    }

    @Override
    public @Nullable DecimalType getProcessCpuUsage(int pid) throws DeviceNotFoundException {
        if (pid > 0) {
            OSProcess process = getProcess(pid);
            double cpuUsageRaw = (process.getKernelTime() + process.getUserTime()) / process.getUpTime();
            BigDecimal cpuUsage = getPercentsValue(cpuUsageRaw);
            return new DecimalType(cpuUsage);
        } else {
            return null;
        }
    }

    @Override
    public @Nullable DecimalType getProcessMemoryUsage(int pid) throws DeviceNotFoundException {
        if (pid > 0) {
            OSProcess process = getProcess(pid);
            long memortInBytes = process.getResidentSetSize();
            long memoryInMB = getSizeInMB(memortInBytes);
            return new DecimalType(memoryInMB);
        } else {
            return null;
        }
    }

    @Override
    public @Nullable StringType getProcessPath(int pid) throws DeviceNotFoundException {
        if (pid > 0) {
            OSProcess process = getProcess(pid);
            String path = process.getPath();
            return new StringType(path);
        } else {
            return null;
        }
    }

    @Override
    public @Nullable DecimalType getProcessThreads(int pid) throws DeviceNotFoundException {
        if (pid > 0) {
            OSProcess process = getProcess(pid);
            int threadCount = process.getThreadCount();
            return new DecimalType(threadCount);
        } else {
            return null;
        }
    }
}
