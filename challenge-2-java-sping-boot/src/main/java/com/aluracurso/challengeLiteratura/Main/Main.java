package com.aluracurso.challengeLiteratura.Main;

import com.aluracurso.challengeLiteratura.models.Author;
import com.aluracurso.challengeLiteratura.models.Book;
import com.aluracurso.challengeLiteratura.models.Data;
import com.aluracurso.challengeLiteratura.models.DataBook;
import com.aluracurso.challengeLiteratura.repository.AuthorRpository;
import com.aluracurso.challengeLiteratura.repository.BookRepository;
import com.aluracurso.challengeLiteratura.services.ApiClient;
import com.aluracurso.challengeLiteratura.services.DataConverter;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Scanner;

@Transactional
public class Main {
    private static final String URL_API = "https://gutendex.com/books/";
    private ApiClient apiClient = new ApiClient();
    private Scanner input = new Scanner(System.in);
    private DataConverter converter = new DataConverter();
    private BookRepository repository;
    private AuthorRpository authorRpository;

    public Main(BookRepository repository, AuthorRpository authorRpository) {
        this.repository = repository;
        this.authorRpository = authorRpository;
    }

    public void showMenu() {
        var option = -1;
        while(option != 0) {
            var menu = """
                     \n####### Menú #######
                   \n1 - Buscar libro
                     2 - Ver lista de libros
                     3 - Ver lista de autores
                     4 - Ver autores vivos en un año
                     5 - Listar libros por idioma
                     
                     0 - Salir
                    """;

            System.out.println(menu);
            option = input.nextInt();
            input.nextLine();

            switch (option) {
                case 1:
                    searchBooks();
                break;

                case 2:
                    showListBook();
                break;

                case 3:
                    showListAuthors();
                break;

                case 4:
                    showAliveAuthorsByYear();
                break;

                case 5:
                    showBooksByLanguage();
                break;

                case 0:
                    System.out.println("Cerrando aplicacion...");
                break;
                default:
                    System.out.println("Opcion invalida, intente nuevamente");
            }
        }
    }

    private void saveBook(DataBook dataBook) {
        // Extraer los datos del autor
        var dataAuthor = dataBook.authors().isEmpty() ? null : dataBook.authors().get(0);

        if (dataAuthor == null) {
            System.out.println("El libro no tiene autor. No se puede guardar.");
            return;
        }

        // Buscar el autor en la base de datos
        Author author = authorRpository.findByNameWithBooks(dataAuthor.name())
                .stream()
                .findFirst()
                .orElseGet(() -> {
                    // Crear y guardar el autor si no existe
                    Author newAuthor = new Author(dataAuthor.name(), dataAuthor.birthYear(), dataAuthor.deathYear());
                    authorRpository.save(newAuthor);
                    return newAuthor;
                });

        // Crear y guardar el libro
        Book book = new Book(dataBook.title(), author, dataBook.languages(), dataBook.downloand());
        repository.save(book);
        System.out.println("Libro guardado exitosamente: " + book.getTitle());
    }


    private DataBook searchBooks() {
        System.out.println("¿Que libro desea buscar hoy?");
        var nameBook = input.nextLine();
        var json = apiClient.getData(URL_API + "?search=" + nameBook.replace(" ", "+"));

        var foundBook = converter.getData(json, Data.class);
        Optional<DataBook> book = foundBook.bookList().stream()
                .filter(b -> b.title().toUpperCase().contains(nameBook.toUpperCase()))
                .findFirst();

        if (book.isPresent()) {
            System.out.println("Su libro es: ");
            System.out.println(book.get());

            saveBook(book.get());
        } else {
            System.out.println("El libro " + book + " no se ha podido encontrar");
        }
        return null;
    }

    @Transactional
    public void showListBook() {
        var books = repository.findAllWithLanguages();

        if (books.isEmpty()) {
            System.out.println("No hay libros registrados en la base de datos.");
        } else {
            System.out.println("####### Lista de Libros #######");
            books.forEach(book -> {
                System.out.println("\nTítulo: " + book.getTitle());
                System.out.println("Idiomas: " + book.getLanguages());
                System.out.println("Descargas: " + book.getDownloads());
                System.out.println("-------------------------");
            });
        }
    }


    @Transactional
    public void showListAuthors() {
        System.out.println("####### Lista de Autores #######");

        // Cargar autores con sus libros
        authorRpository.findAll().forEach(author -> {
            System.out.println("\nNombre: " + author.getName());
            System.out.println("Fecha de nacimiento: " + author.getBirthYear());
            System.out.println("Fecha de fallecimiento: " + author.getDeathYear());
            System.out.println("Libros: ");

            // Verificar si el autor tiene libros
            if (author.getBooks() != null && !author.getBooks().isEmpty()) {
                author.getBooks().forEach(book -> System.out.println("- " + book.getTitle()));
                System.out.println("-------------------------");
            } else {
                System.out.println("No hay libros asociados.");
            }
        });
    }

    public void showAliveAuthorsByYear() {
        System.out.println("Ingrese el año para ver los autores vivos: ");
        int year = input.nextInt();
        input.nextLine();

        var authors = authorRpository.findAliveAuthorsByYear(year);

        if (authors.isEmpty()) {
            System.out.println("No hay autores vivos en el año " + year);
        } else {
            System.out.println("####### Autores vivos en el año " + year + " #######");
            authors.forEach(author -> {
                System.out.println("Nombre: " + author.getName());
                System.out.println("Fecha de nacimiento: " + author.getBirthYear());
                System.out.println("Fecha de fallecimiento: " + (author.getDeathYear() == null ? "Sigue vivo" : author.getDeathYear()));
                System.out.println("Libros: ");
                if (author.getBooks() != null && !author.getBooks().isEmpty()) {
                    author.getBooks().forEach(book -> System.out.println("- " + book.getTitle()));
                } else {
                    System.out.println("No hay libros asociados.");
                }
                System.out.println("-------------------------");
            });
        }
    }

    public void showBooksByLanguage() {
        System.out.println("Ingrese el idioma para buscar libros: ");
        System.out.println("""
            Ejemplos: 
            es - Español
            en - Inglés
            fr - Francés
            pt - Portugués
            """);
        String language = input.nextLine();

        var books = repository.findBooksByLanguage(language);

        if (books.isEmpty()) {
            System.out.println("No se encontraron libros en el idioma: " + language);
        } else {
            System.out.println("####### Libros en el idioma: " + language + " #######");
            books.forEach(book -> {
                System.out.println("Título: " + book.getTitle());
                System.out.println("Autor: " + book.getAuthor().getName());
                System.out.println("Descargas: " + book.getDownloads());
                System.out.println("-------------------------");
            });
        }
    }
}
