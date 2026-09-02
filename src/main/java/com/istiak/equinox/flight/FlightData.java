package com.istiak.equinox.flight;

import java.util.UUID;

/**

* Stores runtime information about an active Equinox flight.
*
* FlightData is intentionally temporary.
*
* It is NOT permanently saved to disk because a horse should not
* remain in "flying mode" after a server restart.
  */
  public final class FlightData {

  /*

  * ============================================================
  * IDENTIFICATION
  * ============================================================
    */

  private final UUID horseId;

  private final UUID ownerId;

  /*

  * ============================================================
  * FLIGHT STATE
  * ============================================================
    */

  private boolean flying;

  private boolean takingOff;

  private boolean landing;

  /*

  * ============================================================
  * TIMING
  * ============================================================
    */

  private long flightStartedAt;

  /*

  * ============================================================
  * CONSTRUCTOR
  * ============================================================
    */

  public FlightData(
  UUID horseId,
  UUID ownerId
  ) {


   this.horseId = horseId;

   this.ownerId = ownerId;

   this.flying = false;

   this.takingOff = false;

   this.landing = false;

   this.flightStartedAt = 0L;

  }

  /*

  * ============================================================
  * BASIC GETTERS
  * ============================================================
    */

  public UUID getHorseId() {


   return horseId;


  }

  public UUID getOwnerId() {


   return ownerId;


  }

  /*

  * ============================================================
  * FLIGHT STATUS
  * ============================================================
    */

  public boolean isFlying() {


   return flying;


  }

  public boolean isTakingOff() {


   return takingOff;


  }

  public boolean isLanding() {


   return landing;


  }

  public long getFlightStartedAt() {


   return flightStartedAt;


  }

  /*

  * ============================================================
  * START TAKEOFF
  * ============================================================
    */

  public void startTakeoff() {


   this.takingOff = true;

   this.landing = false;


  }

  /*

  * ============================================================
  * START FLIGHT
  * ============================================================
    */

  public void startFlight() {


   this.flying = true;

   this.takingOff = false;

   this.landing = false;

   this.flightStartedAt =
           System.currentTimeMillis();


  }

  /*

  * ============================================================
  * START LANDING
  * ============================================================
    */

  public void startLanding() {


   this.landing = true;

   this.takingOff = false;


  }

  /*

  * ============================================================
  * STOP FLIGHT
  * ============================================================
    */

  public void stopFlight() {


   this.flying = false;

   this.takingOff = false;

   this.landing = false;

   this.flightStartedAt = 0L;


  }
  }
