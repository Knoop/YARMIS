package com.yarmis.core;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map.Entry;

import com.yarmis.core.Communication.Request;
import com.yarmis.core.Communication.Response;

/**
 * A Request queue contains Requests and Results. Requests can be added to the
 * queue. If they already are in the queue you'll get the stored Result.
 * Otherwise a new entry will be made and the accompanying Result is returned.
 */
public class RequestSender {

    private Thread requestSender = new Thread(new Runnable() {

	@Override
	public void run() {
	    while (true) {
		try {
		    synchronized (order) {
			order.wait();
		    }

		    while (order.size() > 0)
			synchronized (order) {
			    Element next = getNext();
			    try {
				next.connection.sendRequest(next.request);
			    } catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			    }
			}

		} catch (InterruptedException e) {
		    e.printStackTrace();
		}

	    }

	}

    });

    /**
     * The order of the requests.
     */
    private final LinkedList<Element> order;

    /**
     * Mapping that contains all Request that have been send and are pending a
     * response. The Mapping maps Requests to Elements. These Elements contain
     * all important information about where the response should go to.
     */
    private final HashMap<Request, Element> pending;

    public RequestSender() {
	this.order = new LinkedList<Element>();
	this.pending = new HashMap<Request, Element>();
	this.requestSender.start();
    }

    /**
     * Let the {@code Request} be send to the given {@code Connection}. This
     * will yield a {@code Result} that can be used to be notified when the
     * {@code Request} has been processed.
     * 
     * @param request
     *            The Request to send.
     * @return The Result object associated with the Request. Whoever made the
     *         request should call {@code get()} on this object to wait until
     *         the data is available.
     */
    public Result send(Request request, Connection connection) {

	Element element = new Element(request, connection);
	this.order.addLast(element);

	synchronized (this.order) {
	    this.order.notify();
	}

	return element.result;

    }

    /**
     * <p>
     * Get the next request from the queue.
     * </p>
     * <p>
     * This will remove the Request from the order. It will however not remove
     * the Request - Result combination from the mapping. This is done when the
     * Response is reported.
     * </p>
     * 
     * @return The next Element from the queue.
     */
    private Element getNext() {

	Element next = this.order.removeFirst();
	this.pending.put(next.request, next);
	return next;
    }

    /**
     * Report a Response to this queue. This will remove the Request - Result
     * combination from the mapping. It will also push the response to the
     * result such that other threads waiting on the response will get back to
     * work.
     * 
     * @param response
     *            The Response that came in
     * @throws IllegalStateException
     *             If there was no Result associated with the Request for the
     *             Response.
     */
    public void report(Response response) {

	Element element = this.pending.remove(response.request);

	if (element != null)
	    element.report(response);
    }

    /**
     * Clears the RequestQueue of all pending requests. This will make sure that
     * all result will at least unlock. They will unlock because of an
     * {@code RequestClearedException}.
     */
    public void clear() {

	Iterator<Entry<Request, Element>> iterator = this.pending.entrySet()
		.iterator();

	while (iterator.hasNext()) {
	    Element element = iterator.next().getValue();
	    element.clear();
	    iterator.remove();
	}

    }

    /**
     * Class that couples a Request to a Result.
     * 
     * @author Maurice
     * 
     */
    public class Element {
	public final Request request;
	public final Result result;
	public final Connection connection;

	private Element(Request request, Connection connection) {
	    this.request = request;
	    this.result = new Result();
	    this.connection = connection;
	}

	private void report(Response response) {

	    this.result.set(response);
	}

	private void clear() {

	}

    }
}
