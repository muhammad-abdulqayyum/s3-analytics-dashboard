import React, {useState,useRef,useEffect} from 'react';
import {createRoot} from 'react-dom/client';
import './style.css';
function App(){
 const [start,S]=useState('2026-10-01'),[end,E]=useState('2026-10-07');
 const [user,U]=useState('demo'),[password,P]=useState('demo-change-me');
 const [rows,R]=useState([]),[status,T]=useState('Ready'),[busy,B]=useState(false);
 const active=useRef(null),controller=useRef(null);
 useEffect(()=>()=>controller.current?.abort(),[]);
 async function api(path,method='GET',body,signal){
  const headers={Authorization:'Basic '+btoa(user+':'+password)};
  if(method!=='GET'){
   const tokenResponse=await fetch('/api/csrf',{headers,signal});
   if(!tokenResponse.ok)throw new Error('Authentication failed');
   const csrf=await tokenResponse.json();headers[csrf.headerName]=csrf.token;
  }
  if(body)headers['Content-Type']='application/json';
  const response=await fetch('/api'+path,{method,headers,body:body?JSON.stringify(body):undefined,signal});
  if(!response.ok)throw new Error('Request failed ('+response.status+'). Check credentials, Redis and backend logs.');
  return response.status===204?null:response.json();
 }
 async function run(event){
  event.preventDefault();B(true);R([]);T('Submitting query…');
  const abort=new AbortController();controller.current=abort;
  try{
   let result=await api('/queries','POST',{start,end},abort.signal);active.current=result.jobId;
   const deadline=Date.now()+120000;
   while(result.status==='RUNNING'){
    if(Date.now()>deadline)throw new Error('Query exceeded two minutes. Cancel it or check the backend.');
    T('Query running — reading relevant date partitions');
    await new Promise(resolve=>setTimeout(resolve,1000));abort.signal.throwIfAborted();
    result=await api('/queries/'+result.jobId,'GET',undefined,abort.signal);
   }
   if(result.status!=='SUCCEEDED')throw new Error('Query '+result.status.toLowerCase());
   R(result.rows);T(result.cached?'Served from Redis cache':'Query complete · cached for 5 minutes');active.current=null;
  }catch(error){T(error.name==='AbortError'?'Request stopped':error.message);}
  finally{B(false);}
 }
 async function cancel(){const id=active.current;controller.current?.abort();if(id){try{await api('/queries/'+id,'DELETE');T('Query cancelled');active.current=null;}catch(e){T(e.message);}}}
 const total=rows.reduce((sum,r)=>sum+r.transactions,0),amount=rows.reduce((sum,r)=>sum+Number(r.amount),0);
 return <main><header><span className="badge">SPRING BOOT / REDIS / S3</span><h1>Transaction intelligence</h1><p>A small dashboard for a large data lake. Demo mode uses synthetic data.</p></header>
 <form onSubmit={run}><div className="filters"><label>Username<input value={user} onChange={e=>U(e.target.value)} disabled={busy}/></label><label>Password<input type="password" value={password} onChange={e=>P(e.target.value)} disabled={busy}/></label><label>From<input required type="date" value={start} onChange={e=>S(e.target.value)} disabled={busy}/></label><label>Through<input required type="date" value={end} onChange={e=>E(e.target.value)} disabled={busy}/></label></div><button disabled={busy}>Run analytics</button>{busy&&<button type="button" className="secondary" onClick={cancel}>Cancel query</button>}</form>
 <p role="status">{status}</p><section className="metrics"><article><small>TRANSACTIONS</small><strong>{total.toLocaleString()}</strong></article><article><small>TOTAL VALUE</small><strong>{amount.toLocaleString('en-US',{style:'currency',currency:'USD'})}</strong></article><article><small>DAYS RETURNED</small><strong>{rows.length}</strong></article></section>
 <section className="panel"><h2>Daily volume</h2>{rows.length===0?<p>Select up to 31 days and run a query.</p>:<><div className="chart" aria-label="Daily transaction volume">{rows.map(r=><div key={r.date} className="bar-row"><span>{r.date}</span><div className="bar" style={{width:(r.transactions/Math.max(...rows.map(x=>x.transactions))*65)+'%'}}/><span>{r.transactions.toLocaleString()}</span></div>)}</div><table><thead><tr><th>Date</th><th>Transactions</th><th>Value (USD)</th></tr></thead><tbody>{rows.map(r=><tr key={r.date}><td>{r.date}</td><td>{r.transactions.toLocaleString()}</td><td>{Number(r.amount).toLocaleString('en-US',{style:'currency',currency:'USD'})}</td></tr>)}</tbody></table></>}</section><footer>Sample project · Not connected to banking systems · No scale benchmark claimed</footer></main>;
}
createRoot(document.getElementById('root')).render(<App/>);
