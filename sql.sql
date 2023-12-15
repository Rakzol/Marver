USE Mochis;
GO

CREATE TABLE posiciones(
	id BIGINT PRIMARY KEY IDENTITY(1,1) NOT NULL,
	usuario INT NOT NULL,
	latitud FLOAT CHECK (latitud >= -90.0 AND latitud <= 90.0) NOT NULL,
	longitud FLOAT CHECK (longitud >= -180.0 AND longitud <= 180.0) NOT NULL,
	fecha DATETIME NOT NULL
);