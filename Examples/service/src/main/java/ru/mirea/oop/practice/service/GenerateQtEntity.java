package ru.mirea.oop.practice.service;

import com.google.common.base.CaseFormat;
import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

public final class GenerateQtEntity {
    private static final String ENDL = System.getProperty("line.separator");

    public static void main(String[] args) throws IOException {
        final Gson gson = new GsonBuilder().create();
        DefClass[] classes = gson.fromJson(new InputStreamReader(GenerateQtEntity.class.getResourceAsStream("/.entities.json")),
                DefClass[].class);
        File directory = new File(".generated");
        directory.mkdir();
        for (DefClass def : classes) {
            StringBuilder builder = new StringBuilder();
            builder.append("#ifndef ").append(def.getDefineName()).append(ENDL);
            builder.append("#define ").append(def.getDefineName()).append(ENDL);
            builder.append("/** Autogenerated */");
            builder.append(ENDL);
            builder.append("#include <QObject>").append(ENDL);
            builder.append("#include <QUuid>").append(ENDL);
            builder.append("#include <QUrl>").append(ENDL);
            builder.append("#include <QDateTime>").append(ENDL);
            builder.append("#include <QMetaType>").append(ENDL);
            builder.append(ENDL);
            for (String include: def.includes) {
                builder.append("#include \"").append(include).append("\"").append(ENDL);
            }
            builder.append(ENDL);
            builder.append("namespace Entities {").append(ENDL);
            builder.append("class ").append(def.getClassName()).append(" final: public QObject {").append(ENDL);
            builder.append("    Q_OBJECT").append(ENDL);
            for (Property property : def.properties) {
                builder.append("    Q_PROPERTY(")
                        .append(property.type).append(" ")
                        .append(property.name).append(" ")
                        .append("READ ").append(property.getGetter()).append(" ")
                        .append("WRITE ").append(property.getSetter()).append(" ")
                        .append("NOTIFY ").append(property.getGetter()).append("Changed")
                        .append(")").append(ENDL);
            }
            builder.append("public:").append(ENDL);
            builder.append("    ").append(def.getClassName()).append("(QObject *parent = nullptr): QObject(parent) {}").append(ENDL);
            builder.append("    ").append(def.getClassName()).append("(const ")
                    .append(def.getClassName()).append(" &other)").append(ENDL);
            StringBuilder spacer = new StringBuilder("    ");
            for (int i = 0; i < def.getClassName().length(); ++i) {
                spacer.append(" ");
            }
            builder.append(spacer.toString());
            builder.append(": QObject(other.parent()), ").append(ENDL);
            spacer.append("  ");
            int k = 0;
            for (Property property : def.properties) {
                if (k > 0)
                    builder.append(", ").append(ENDL);
                builder.append(spacer.toString());
                builder.append("_").append(property.getGetter()).append("(other._").append(property.getGetter()).append(")");
                ++k;
            }
            builder.append(" {}").append(ENDL).append(ENDL);
            //operator=
            builder.append("    const ").append(def.getClassName()).append(" &operator=(const ")
                    .append(def.getClassName()).append(" &other) {").append(ENDL);
            builder.append("        setParent(other.parent());").append(ENDL);
            for (Property property : def.properties) {
                builder.append("        _")
                        .append(property.getGetter()).append(" = other._")
                        .append(property.getGetter()).append(";")
                        .append(ENDL);
            }
            builder.append("        return (*this);").append(ENDL);
            builder.append("    }").append(ENDL);

            builder.append("    const QString tableName() { return QString::fromUtf8(\"").append(def.getClassName()).append("\"); }").append(ENDL);
            builder.append(ENDL);

            for (Property property : def.properties) {
                builder.append("    /** Getter ").append(property.getGetter()).append("*/").append(ENDL);
                builder.append("    ")
                        .append(property.type)
                        .append(" ")
                        .append(property.getGetter())
                        .append("() { return _")
                        .append(property.getGetter())
                        .append("; }")
                        .append(ENDL).append(ENDL);
                builder.append("    /** Setter ").append(property.getGetter()).append("*/").append(ENDL);
                builder.append("    ")
                        .append("void")
                        .append(" ")
                        .append(property.getSetter())
                        .append("(const ")
                        .append(property.type)
                        .append(" &")
                        .append(property.getGetter())
                        .append(") { ").append(ENDL)
                        .append("        _")
                        .append(property.getGetter())
                        .append(" = ").append(property.getGetter())
                        .append(";").append(ENDL)
                        .append("        emit ")
                        .append(property.getGetter()).append("Changed(")
                        .append("_").append(property.getGetter())
                        .append(");").append(ENDL)
                        .append("    }")
                        .append(ENDL);
            }

            //signals
            builder.append("Q_SIGNALS:").append(ENDL);
            for (Property property : def.properties) {
                builder.append("    void ")
                        .append(property.getGetter()).append("Changed(")
                        .append(property.type).append(" &")
                        .append(")")
                        .append(";")
                        .append(ENDL);
            }

            //fields
            builder.append("private:").append(ENDL);
            for (Property property : def.properties) {
                builder.append("    ")
                        .append(property.type)
                        .append(" _")
                        .append(property.getGetter())
                        .append(";")
                        .append(ENDL);
            }

            builder.append("};").append(ENDL);
            builder.append("}").append(ENDL);
            builder.append("Q_DECLARE_METATYPE(Entities::").append(def.getClassName()).append(")").append(ENDL);
            builder.append("#endif /**").append(def.getDefineName()).append("*/");
            Files.write(builder.toString(), new File(directory, def.getFileName() + ".h"), Charsets.UTF_8);
        }
    }

    private static final class DefClass {
        public String name;
        public Property[] properties;
        public String [] includes = new String[0];

        public String getFileName() {
            return name.replace("_", "").toLowerCase();
        }

        public String getDefineName() {
            return getFileName().toUpperCase() + "_H";
        }

        public String getClassName() {
            return name;//CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, name);
        }
    }

    private static final class Property {
        public String type;
        public String name;


        public String getGetter() {
            return CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, name);
        }

        public String getSetter() {
            return "set" + CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, name);
        }
    }
}
