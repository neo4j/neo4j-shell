package org.neo4j.shell.test.bolt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;

import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.exceptions.NoSuchRecordException;
import org.neo4j.driver.internal.value.BooleanValue;
import org.neo4j.driver.summary.ResultSummary;
import org.neo4j.shell.test.Util;

/**
 * A fake Result with fake records and fake values
 */
class FakeResult implements Result
{

    public static final FakeResult PING_SUCCESS = new FakeResult( Collections.singletonList( FakeRecord.of( "success", BooleanValue.TRUE ) ) );
    private final List<Record> records;
    private int currentRecord = -1;

    FakeResult() {
        this(new ArrayList<>());
    }

    FakeResult(List<Record> records) {
        this.records = records;
    }

    @Override
    public List<String> keys() {
        return records.stream().map(r -> r.keys().get(0)).collect(Collectors.toList());
    }

    @Override
    public boolean hasNext() {
        return currentRecord + 1 < records.size();
    }

    @Override
    public Record next() {
        currentRecord += 1;
        return records.get(currentRecord);
    }

    @Override
    public Record single() throws NoSuchRecordException {
        if (records.size() == 1) {
            return records.get(0);
        }
        throw new NoSuchRecordException("There are more than records");
    }

    @Override
    public Record peek() {
        throw new Util.NotImplementedYetException("Not implemented yet");
    }

    @Override
    public Stream<Record> stream()
    {
        return records.stream();
    }

    @Override
    public List<Record> list() {
        return records;
    }

    @Override
    public <T> List<T> list(Function<Record, T> mapFunction) {
        throw new Util.NotImplementedYetException("Not implemented yet");
    }

    @Override
    public ResultSummary consume() {
        return new FakeResultSummary();
    }

    /**
     * Supports fake parsing of very limited cypher statements, only for basic test purposes
     */
    static FakeResult parseStatement(@Nonnull final String statement) {

        if ( isPing( statement ) ) {
            return PING_SUCCESS;
        }

        Pattern returnAsPattern = Pattern.compile("^return (.*) as (.*)$", Pattern.CASE_INSENSITIVE);
        Pattern returnPattern = Pattern.compile("^return (.*)$", Pattern.CASE_INSENSITIVE);

        // Be careful with order here
        for (Pattern p: Arrays.asList(returnAsPattern, returnPattern)) {
            Matcher m = p.matcher(statement);
            if (m.find()) {
                String value = m.group(1);
                String key = value;
                if (m.groupCount() > 1) {
                    key = m.group(2);
                }
                FakeResult statementResult = new FakeResult();
                statementResult.records.add(FakeRecord.of(key, value));
                return statementResult;
            }
        }
        throw new IllegalArgumentException("No idea how to parse this statement");
    }

    private static boolean isPing( @Nonnull String statement )
    {
        return statement.trim().equalsIgnoreCase( "CALL db.ping()" );
    }
}
