package asclib.util;

public class AsyncTask {
	
    /**
     * Override this method to perform a computation on a background thread. The
     * specified parameters are the parameters passed to {@link #execute}
     * by the caller of this task.
     *
     * @param params The parameters of the task.
     *
     * @return A result, and pass it to #onPostExecute as a parameter.
     *
     * @see #onPreExecute
     * @see #onPostExecute
     */
	public Object doInBackground() { return null; }
	
    /**
     * Runs on the main thread before {@link #doInBackground}.
     *
     * @see #onPostExecute
     * @see #doInBackground
     */
	public void onPreExecute() {}
	
    /**
     * <p>Runs on the main thread after {@link #doInBackground}. The
     * specified result is the value returned by {@link #doInBackground}.</p>
     *
     * @param result The result of the operation computed by {@link #doInBackground}.
     *
     * @see #onPreExecute
     * @see #doInBackground
     */
	public void onPostExecute(Object result) {}
}


