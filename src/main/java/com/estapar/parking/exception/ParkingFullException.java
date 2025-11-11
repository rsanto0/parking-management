package com.estapar.parking.exception;

public class ParkingFullException extends RuntimeException {
    public ParkingFullException(String sector) {
        super("Estacionamento lotado no setor: " + sector);
    }
}