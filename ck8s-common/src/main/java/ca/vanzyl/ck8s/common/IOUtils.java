package ca.vanzyl.ck8s.common;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.CopyOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class IOUtils
{

    public static void copyFolder(Path src, Path dest)
            throws IOException
    {
        try (Stream<Path> stream = Files.walk(src)) {
            stream.forEach(source -> copy(source, dest.resolve(src.relativize(source))));
        }
    }

    public static void copy(Path source, Path dest)
    {
        try {
            Files.copy(source, dest, REPLACE_EXISTING);
        }
        catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public static void copy(Path src, Path dst, List<String> ignorePattern, CopyOption... options)
            throws IOException
    {
        Files.walkFileTree(src, new SimpleFileVisitor<>()
        {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
            {
                if (dir != src && anyMatch(src.relativize(dir).toString(), ignorePattern)) {
                    return FileVisitResult.SKIP_SUBTREE;
                }

                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                    throws IOException
            {
                if (file != src && anyMatch(src.relativize(file).toString(), ignorePattern)) {
                    return FileVisitResult.CONTINUE;
                }

                Path a = file;
                Path b = dst.resolve(src.relativize(file));

                Path parent = b.getParent();
                if (!Files.exists(parent)) {
                    Files.createDirectories(parent);
                }

                if (Files.isSymbolicLink(file)) {
                    Path link = Files.readSymbolicLink(file);
                    Path target = file.getParent().resolve(link).normalize();

                    if (!target.startsWith(src)) {
                        throw new IOException("Symlinks outside the base directory are not supported: " + file + " -> " + target);
                    }

                    if (Files.notExists(target)) {
                        // missing target
                        return FileVisitResult.CONTINUE;
                    }

                    Files.createSymbolicLink(b, link);
                    return FileVisitResult.CONTINUE;
                }

                Files.copy(a, b, options);

                return FileVisitResult.CONTINUE;
            }
        });
    }

    public static boolean deleteRecursively(Path p)
    {
        if (!Files.exists(p)) {
            return false;
        }

        try {
            if (!Files.isDirectory(p)) {
                Files.delete(p);
                return true;
            }

            Files.walkFileTree(p, new SimpleFileVisitor<>()
            {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                        throws IOException
                {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc)
                        throws IOException
                {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });

            return true;
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean anyMatch(String what, List<String> patterns)
    {
        if (patterns == null) {
            return false;
        }

        return patterns.stream().anyMatch(what::matches);
    }

    public static String read(InputStream is) {
        return new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8)).lines()
                .collect(Collectors.joining(System.lineSeparator()));
    }
}
