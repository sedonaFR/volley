package fr.sedona.volley.manager;

/**
 * If no Error to parse, use a Void type
 * @param <T> Data type response
 * @param <E> Data type error
 */
public interface QueryCallback <T,E> {

    public void onQueryFinished(int idQuery, ResultInfo queryInfo, T data, E error);
}
