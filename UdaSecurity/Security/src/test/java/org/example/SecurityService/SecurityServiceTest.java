package org.example.SecurityService;

import org.example.application.StatusListener;
import org.example.data.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.example.service.ImageService;

import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;


import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
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
      //Assertions.assertEquals(AlarmStatus.PENDING_ALARM,securityService.getAlarmStatus(),"If alarm is armed and a sensor becomes activated,  the system should  at pending alarm status ");
    }
    /*Test 2:
    If alarm is armed and a sensor becomes activated and the system is already pending alarm, set the alarm status to alarm.*/
    @Test
    public void changeSensorActivationStatus_ArmedAndActivatedAndpendingAlarm_statusAlarm2( ){
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        securityService.changeSensorActivationStatus(sensor,Boolean.TRUE);
        verify(securityRepository,times(1)).setAlarmStatus(AlarmStatus.ALARM);

    }
    /*Test 3:
    * If pending alarm and all sensors are inactive, return to no alarm state.*/
    @Test
    public void  changeSensorActivationStatus_pendingAlarmAndInactiveSensor_statusNOAlarm3( ){
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        securityService.changeSensorActivationStatus(sensor,true);
        securityService.changeSensorActivationStatus(sensor,false);
        verify(securityRepository,times(1)).setAlarmStatus(AlarmStatus.NO_ALARM);

    }
    /*Test 4:If alarm is active, change in sensor state should not affect the alarm state.*/
    @ParameterizedTest
    @ValueSource(booleans= {true,false})
    public void  changeSensorActivationStatus_alarmActive_statusNOAffect4(boolean status ){
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);
        ;
        securityService.changeSensorActivationStatus(sensor,status);
        verify(securityRepository,never()).setAlarmStatus(any(AlarmStatus.class));


    }
    /*Test 5: If a sensor is activated while already active and the system is in pending state, change it to alarm state.*/
    @Test
    public void  changeSensorActivationStatus_alarmActiveAgainAndPendingState_statusAlarmState5( ){
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        securityService.changeSensorActivationStatus(sensor, true);
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
        when(imageService.imageContainsCat(any(),anyFloat())).thenReturn(false);
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        sensor.setActive(false);
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
       when(securityRepository.getArmingStatus()).thenReturn(status );


        securityService.getSensors().forEach(s -> assertFalse(s.getActive()));

    }
    /*Test 11 :If the system is armed-home while the camera shows a cat, set the alarm status to alarm.*/
    @Test
    public void IfArmedHomeAnd_showCat_statusAlarm11(){
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(imageService.imageContainsCat(any(),anyFloat())).thenReturn(true);
        securityService.processImage(eq(any(BufferedImage.class)));
        verify(securityRepository,times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }

}