package com.oskarro.oskarito.track;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface TrackRepository extends CrudRepository<Track, Integer> {

    List<Track> findAll();

    Optional<Track> findById(Integer id);

    Track save(Track track);


    /** Single tracks of a genre; sets have their own section. */
    @Query("select t from Track t where t.genre = :genre and (t.kind is null or t.kind <> 'set')")
    List<Track> findTracksByGenre(@Param("genre") String genre);

    @Query("select t from Track t where t.kind = 'set'")
    List<Track> findSets();



    List<Track> findTracksByPlaylistId(Integer id);

    List<Track> findTracksByVideoId(Integer id);

    List<Track> findByAuthor_IdOrderByIdDesc(Integer authorId);

    long countByAuthor_Id(Integer authorId);

    @Query("select t.author.id, count(t) from Track t where t.author is not null group by t.author.id")
    List<Object[]> countGroupedByAuthor();

    @Query("select t.author.id, count(t) from Track t where t.author is not null and t.createdAt >= :from group by t.author.id")
    List<Object[]> countGroupedByAuthorSince(@Param("from") Instant from);

    @Query("select t.id, t.url from Track t where t.genre is not null and t.genre <> '' and t.url is not null and t.url <> '' order by t.id desc")
    List<Object[]> communityIdsAndUrls();

    @Query("select count(t) from Track t where t.genre is not null and t.genre <> '' and t.url is not null and t.url <> ''")
    long countCommunity();

    @Query("select t.createdAt from Track t where t.genre is not null and t.genre <> '' and t.url is not null and t.url <> '' and t.createdAt >= :from")
    List<Instant> findCommunityCreatedSince(@Param("from") Instant from);

    void deleteById(Integer id);

    List<Track> findTop200ByCreatedAtNotNullOrderByCreatedAtDesc();

    List<Track> findTop20ByAuthor_IdOrderByIdDesc(Integer authorId);
}
