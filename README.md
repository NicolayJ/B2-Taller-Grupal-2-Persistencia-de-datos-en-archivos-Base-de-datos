
### **1. Estructura general del proyecto**
El proyecto está organizado siguiendo buenas prácticas de desarrollo en Scala, separando la lógica en módulos para mantener el código limpio y modular. 

- **resources/application.conf**: Archivo de configuración para manejar credenciales de la base de datos.
- **resources/data/estudiantes.csv**: Archivo CSV con los datos de los estudiantes.
- **scala/config**: Módulo de configuración, principalmente para la conexión a la base de datos.
- **scala/dao**: Data Access Object (DAO), encargado de interactuar con la base de datos.
- **scala/models**: Modelo de datos que representa a un estudiante.
- **scala/services**: Contiene la lógica principal para ejecutar las funcionalidades del programa.

---

### **2. Detalles de cada componente**

#### **2.1. Configuración (`resources/application.conf`)**
El archivo `application.conf` contiene las credenciales para conectarse a la base de datos:

```hocon
db {
  driver = "com.mysql.cj.jdbc.Driver"
  url = "jdbc:mysql://localhost:3306/movie"
  user = "root"
  password = "camomilla10.@"
}
```

- `driver`: Especifica el controlador JDBC para MySQL.
- `url`: URL de conexión que apunta a la base de datos llamada `movie`.
- `user` y `password`: Credenciales para acceder a la base de datos.

#### **2.2. CSV de estudiantes (`resources/data/estudiantes.csv`)**
Este archivo contiene los datos iniciales de los estudiantes. Será procesado para extraer información y almacenarla en la base de datos.

---

#### **2.3. Configuración de la base de datos (`config/Database.scala`)**
Este módulo configura la conexión a la base de datos utilizando `HikariCP`, un pool de conexiones eficiente:

```scala
object Database {
  def transactor: Resource[IO, HikariTransactor[IO]] = {
    val config = ConfigFactory.load().getConfig("db") // Carga el bloque "db" desde application.conf
    HikariTransactor.newHikariTransactor[IO](
      config.getString("driver"),
      config.getString("url"),
      config.getString("user"),
      config.getString("password"),
      ExecutionContext.global
    )
  }
}
```

**Explicación:**
- **`ConfigFactory.load().getConfig("db")`**: Carga la configuración de la base de datos que se encuentra en el archivo `application.conf` en el bloque `db`.
- **`HikariTransactor.newHikariTransactor[IO]`**: Crea un transactor, que es una forma de manejar transacciones en la base de datos. Este transactor usa el pool de conexiones HikariCP, que es muy eficiente para gestionar múltiples conexiones simultáneas.
- **`ExecutionContext.global`**: Es el contexto de ejecución para realizar operaciones asíncronas. Permite que las operaciones de la base de datos no bloqueen el hilo principal.

Este transactor es lo que proporciona la capacidad de ejecutar operaciones en la base de datos de forma eficiente.
#### **2.4. Modelo de datos (`models/Estudiante.scala`)**
Define la estructura de un estudiante:

```scala
case class Estudiante(
  nombre: String,
  edad: Int,
  calificacion: Int,
  genero: String
)
```

- Usa una `case class` para representar un estudiante con los atributos indicados.

---

#### **2.5. Operaciones de base de datos (`dao/EstudianteDAO.scala`)**
Este módulo contiene las funciones necesarias para interactuar con la tabla de estudiantes en la base de datos:

```scala
object EstudianteDAO {
  def insert(estudiante: Estudiante): ConnectionIO[Int] = {
    sql"""
     INSERT INTO estudiantes (nombre, edad, calificacion, genero)
     VALUES (
       ${estudiante.nombre},
       ${estudiante.edad},
       ${estudiante.calificacion},
       ${estudiante.genero}
     )
    """.update.run
  }

  def insertAll(estudiantes: List[Estudiante]): IO[List[Int]] = {
    Database.transactor.use { xa =>
      estudiantes.traverse(est => insert(est).transact(xa))
    }
  }
}
```

**Explicación:**
- **`insert`**: Esta función toma un objeto `Estudiante` y genera una consulta SQL que inserta ese estudiante en la base de datos. La función `sql""" ... """` es parte de la librería *Doobie* y permite escribir consultas SQL de forma segura.
  - **`update.run`**: Ejecuta la actualización en la base de datos, que es la operación `INSERT` en este caso.
  
- **`insertAll`**: Esta función inserta una lista de estudiantes en la base de datos.
  - **`Database.transactor.use`**: Usa el transactor (conexión a la base de datos) para ejecutar las consultas de manera segura.
  - **`estudiantes.traverse`**: La función `traverse` es parte de la librería *Cats*, que transforma y ejecuta una lista de efectos (en este caso, la inserción de estudiantes) de forma secuencial y segura.
  - **`transact(xa)`**: Ejecuta la operación dentro de una transacción.


---

#### **2.6. Lógica principal (`services/Main.scala`)**
Este archivo contiene el flujo principal de ejecución:

```scala
object Main extends IOApp.Simple {
  val path2DataFile2 = "src/main/resources/data/estudiantes.csv"

  val dataSource = new File(path2DataFile2)
    .readCsv[List, Estudiante](rfc.withHeader.withCellSeparator(','))

  val estudiantes = dataSource.collect {
    case Right(estudiante) => estudiante
  }

  def run: IO[Unit] = for {
    result <- EstudianteDAO.insertAll(estudiantes)
    _ <- IO.println(s"Registros insertados: ${result.size}")
  } yield ()
}
```
**Explicación:**
- **`path2DataFile2`**: Define la ruta del archivo CSV donde se encuentran los datos de los estudiantes.
- **`readCsv[List, Estudiante](rfc.withHeader.withCellSeparator(','))`**: Utiliza la librería *Kantan CSV* para leer el archivo CSV. Convierte cada fila del CSV en un objeto `Estudiante`. El parámetro `rfc.withHeader` indica que el archivo tiene una fila de encabezado, y `withCellSeparator(',')` especifica que las columnas están separadas por comas.
- **`dataSource.collect { case Right(estudiante) => estudiante }`**: Convierte el resultado de la lectura del CSV en una lista de objetos `Estudiante`. Si la conversión falla, se descarta (el uso de `Right` indica éxito en la conversión).
  
- **`run`**: Es el punto de entrada del programa, utilizando *Cats Effect* para manejar efectos secundarios.
  - **`EstudianteDAO.insertAll(estudiantes)`**: Llama a la función para insertar todos los estudiantes en la base de datos.
  - **`IO.println(s"Registros insertados: ${result.size}")`**: Imprime el número de registros insertados en consola.

**Explicación del flujo:**
1. **Leer CSV**:
   - Utiliza `kantan.csv` para leer el archivo CSV y convertir las filas en objetos `Estudiante`.
2. **Insertar datos**:
   - Usa `EstudianteDAO.insertAll` para insertar todos los estudiantes en la base de datos.
3. **Mostrar resultados**:
   - Imprime en consola el número de registros insertados.

---

### **3. Resumen del flujo del programa**
1. Lee el archivo de configuración (`application.conf`) para obtener las credenciales.
2. Procesa el archivo CSV para convertir los datos en objetos `Estudiante`.
3. Inserta los datos en la base de datos MySQL.
4. Imprime la cantidad de registros insertados.

