USE Mochis;
GO

CREATE TABLE rutas_repartidores (
	id INT IDENTITY(1,1) PRIMARY KEY,
	repartidor INT,
	ruta TEXT,
	fecha_inicio DATETIME,
	fecha_fin DATETIME
);

CREATE TABLE pedidos_repartidores (
	id INT PRIMARY KEY IDENTITY(1,1),
	ruta_repartidor INT FOREIGN KEY REFERENCES rutas_repartidores(id),
	folio INT
);