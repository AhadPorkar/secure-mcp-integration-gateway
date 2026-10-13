import { Client, StreamableHTTPClientTransport } from '@modelcontextprotocol/client';

const token = process.env.MCP_TEST_TOKEN;
if (!token) throw new Error('MCP_TEST_TOKEN is required');
const client = new Client(
  { name: 'secure-mcp-gateway-ci', version: '1.0.0' },
  { versionNegotiation: { mode: { pin: '2026-07-28' } } }
);
const transport = new StreamableHTTPClientTransport(new URL('http://127.0.0.1:18096/mcp'), {
  requestInit: { headers: { Authorization: `Bearer ${token}` } }
});
await client.connect(transport);
if (client.getProtocolEra() !== 'modern') throw new Error(`Expected modern MCP era, got ${client.getProtocolEra()}`);
const listed = await client.listTools();
if (!listed.tools.some(t => t.name === 'orders.lookup')) throw new Error('orders.lookup was not listed');
const result = await client.callTool({ name: 'orders.lookup', arguments: { orderId: 'ORD-A100' } });
if (result.isError) throw new Error('orders.lookup returned isError');
console.log('OFFICIAL_MCP_V2_ACCEPTANCE_OK');
await client.close();
