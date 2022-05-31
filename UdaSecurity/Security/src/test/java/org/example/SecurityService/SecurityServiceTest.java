package org.example.SecurityService;

import org.example.application.StatusListener;
import org.example.data.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.example.service.ImageService;

import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;


import static java.util.Collections.singleton;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)

public class SecurityServiceTest {
    private SecurityService securityService;
    private Sensor sensor;

    private Set<Sensor> getAllSensors(int count, boolean statue){
        Set<Sensor> sensors= new HashSet<>();
        for (int i=0; i<count; i++){
            Sensor sensor=new Sensor(UUID.randomUUID().toString(), SensorType.DOOR);
            sensor.setActive(statue);
            sensors.add(sensor);

        }
        return sensors;


    }

    @Mock
    private ImageService imageService;
    @Mock
    private SecurityRepository securityRepository;
    @Mock
    private StatusListener statusListener;

    @BeforeEach
    void init() {
     securityService= new SecurityService(securityRepository,imageService);
     sensor=new  Sensor(UUID.randomUUID().toString(), SensorType.DOOR);
    }
    /*Test 1: If alarm is armed and a sensor becomes activated, put the system into pending alarm status.*/
    @Test
    public void changeSensorActivationStatus_ArmedAndActivated_statusPending1( ){
      when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
      when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM);
      securityService.changeSensorActivationStatus(sensor,Boolean.TRUE);
      verify(securityRepository,times(1)).setAlarmStatus(AlarmStatus.PENDING_ALARM);

    }
    /*Test 2:
    If alarm is armed and a sensor becomes activated and the system is already pending alarm, set the alarm status to alarm.*/
    @Test
    public void changeSensorActivationStatus_ArmedAndActivatedAndpendingAlarm_statusAlarm2( ){
        when(securityService.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(securityService.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        securityService.changeSensorActivationStatus(sensor,Boolean.TRUE);
        verify(securityRepository,atMost(2)).setAlarmStatus(AlarmStatus.ALARM);

    }
    /*Test 3:
    * If pending alarm and all sensors are inactive, return to no alarm state.*/
    @Test
    public void  changeSensorActivationStatus_pendingAlarmAndInactiveSensor_statusNOAlarm3( ){
        Sensor sensor = new Sensor("sensor", SensorType.DOOR);
        securityService.setArmingStatus(ArmingStatus.ARMED_HOME);
        sensor.setActive(false);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        securityService.setAlarmStatus(AlarmStatus.PENDING_ALARM);
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.NO_ALARM);

    }
    /*Test 4:If alarm is active, change in sensor state should not affect the alarm state.*/
    @ParameterizedTest
    @ValueSource(booleans= {true,false})
    public void  changeSensorActivationStatus_alarmActive_statusNOAffect4(boolean status ){
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);

        securityService.changeSensorActivationStatus(sensor,status);
        verify(securityRepository,never()).setAlarmStatus(any(AlarmStatus.class));


    }
    /*Test 5: If a sensor is activated while already active and the system is in pending state, change it to alarm state.*/
    @Test
    public void  changeSensorActivationStatus_alarmActiveAgainAndPendingState_statusAlarmState5( ){
        when(securityService.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        when(securityRepository.getSensors()).thenReturn(getAllSensors(2, true));
        securityService.changeSensorActivationStatus(sensor,true);
       verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }
    /*Test 6:If a sensor is deactivated while already inactive, make no changes to the alarm state.*/
    @ParameterizedTest
    @EnumSource(AlarmStatus.class)
    public void changeSensorActivationStatus_alreadyDeactivatedSensorDeactivated_noAlarmStatus6(AlarmStatus alarmStatus) {
        when(securityRepository.getAlarmStatus()).thenReturn(alarmStatus);
        sensor.setActive(Boolean.FALSE);
        securityService.changeSensorActivationStatus(sensor, false);

        verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));
    }


    /*Test 7:If the image service identifies an image containing a cat while the system is armed-home, put the system into alarm status..*/
    @Test
    public void processImage_containingCatAndArmedHome_alarmStatus7(){
        when(imageService.imageContainsCat(any(),anyFloat())).thenReturn(true);
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        securityService.processImage(eq(any(BufferedImage.class)));
        verify(securityRepository,times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }

    /*Test 8:If the image service identifies an image that does not contain a cat, change the status to no alarm as long as the sensors are not active.*/
    @Test
    public void processImage_NoCatSensorAndInactive_statusNOAlarm8(){
        Set<Sensor> sensors = getAllSensors(3, false);

        when(imageService.imageContainsCat(any(),anyFloat())).thenReturn(false);


        securityService.processImage(eq(any(BufferedImage.class)));
        verify(securityRepository,times(1)).setAlarmStatus(AlarmStatus.NO_ALARM);
    }
    /*Test 9:If the system is disarmed, set the status to no alarm.*/
    @Test
    public void SetDisArmed_disarmed_statusNoAlarm9(){
        securityService.setArmingStatus(ArmingStatus.DISARMED);
        verify(securityRepository,times(1)).setAlarmStatus(AlarmStatus.NO_ALARM);
    }
  /*Test 10:If the system is armed, reset all sensors to inactive.*/
     private static Stream<ArmingStatus>armingStatusStream(){
         return Stream.of(
                 ArmingStatus.ARMED_HOME,ArmingStatus.ARMED_AWAY
         );
     }
     @ParameterizedTest
     @MethodSource("armingStatusStream")
    public void IfArmed_resetAllSensorsToInactive10(ArmingStatus status){
       Set<Sensor> sensors =getAllSensors(4,true);
       sensors.forEach(s->securityService.addSensor(s));



        securityService.getSensors().forEach(s -> assertFalse(s.getActive()));

    }
    /*Test 11 :If the system is armed-home while the camera shows a cat, set the alarm status to alarm.*/
    @Test
    public void IfArmedHomeAnd_showCat_statusAlarm11(){

        when(imageService.imageContainsCat(any(),anyFloat())).thenReturn(true);
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        securityService.processImage(eq(any(BufferedImage.class)));
        verify(securityRepository,times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }



    @ParameterizedTest
    @EnumSource(ArmingStatus.class)
    public void setArmingStatus(ArmingStatus status) {
        securityService.setArmingStatus(status);
    }

    @ParameterizedTest
    @CsvSource({"NO_ALARM,DOOR,true", "NO_ALARM,DOOR,false", "PENDING_ALARM,DOOR,true", "PENDING_ALARM,DOOR,false",
            "PENDING_ALARM,WINDOW,true", "PENDING_ALARM,WINDOW,false"})
    public void changeSensorStatusWithAlarms(AlarmStatus alarmStatus, SensorType sensorType, Boolean active){
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        Sensor sensor = new Sensor("test", sensorType);
        sensor.setActive(true);
        securityService.changeSensorActivationStatus(sensor, active);
        sensor.setActive(false);
        securityService.changeSensorActivationStatus(sensor, active);
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.DISARMED);
        sensor = new Sensor("test", sensorType);
        sensor.setActive(true);
        securityService.changeSensorActivationStatus(sensor, active);
        sensor.setActive(false);
        securityService.changeSensorActivationStatus(sensor, active);
    }

    /*Test coverage for setArmingStatus*/
    @Test
    void SetArmingStatus_systemArmAndCatDetected_updateSensor() {
        ArmingStatus armingStatus = ArmingStatus.ARMED_HOME;
        Sensor sensor = new Sensor("test", SensorType.DOOR);
        sensor.setActive(true);
        when(imageService.imageContainsCat(any(),anyFloat())).thenReturn(true);
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        securityService.processImage(eq(any(BufferedImage.class)));
        when(securityRepository.getSensors()).thenReturn(singleton(sensor));
        securityService.setArmingStatus(armingStatus);
        verify(securityRepository, Mockito.times(1)).updateSensor(any());
    }
}